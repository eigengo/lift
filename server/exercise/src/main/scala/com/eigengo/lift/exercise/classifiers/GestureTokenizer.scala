package com.eigengo.lift.exercise

package classifiers

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.stage.{TerminationDirective, PushStage, Context, Directive}
import breeze.linalg.DenseMatrix
import com.eigengo.lift.exercise.classifiers.svm.{SVMClassifier, SVMModelParser}
import com.typesafe.config.Config
import scala.collection.mutable

/**
 * Streaming stage that buffers events (the sample window). Once the buffer has filled, a callback  action is executed
 * for each new sample message that we receive.
 *
 * @param size   size of the internal buffer and so the sampling window size
 * @param action when buffer is full, action to be performed for each sample that we subsequently receive
 */
class SamplingWindow[A] private (size: Int, action: PartialFunction[List[A], Unit]) extends PushStage[A, A] {
  require(size > 0)

  private val buffer = mutable.Queue[A]()

  override def onPush(elem: A, ctx: Context[A]): Directive = {
    if (!ctx.isFinishing) {
      if (buffer.length == size) {
        // Buffer is full, so enable action callback
        val emit = buffer.dequeue()
        buffer.enqueue(elem)
        action(buffer.toList)
        // Allow old events to propagate downstream
        ctx.push(emit)
      } else {
        // Buffer is not yet full, so keep consuming from our upstream
        buffer.enqueue(elem)
        ctx.pull()
      }
    } else {
      // As the stage is terminating, ensure buffer is flushed into downstream
      buffer.foreach(ctx.push)
      ctx.finish()
    }
  }

  override def onUpstreamFinish(ctx: Context[A]): TerminationDirective = {
    ctx.absorbTermination()
  }
}

object SamplingWindow {

  def apply[A](size: Int)(action: PartialFunction[List[A], Unit]): SamplingWindow[A] = {
    new SamplingWindow(size, action)
  }

}

/**
 * Node that expects a message on each of its inputs. The collection of input messages is then outputted. This merge node
 * can be thought of as a generalised Zip or ZipWith node.
 *
 * @param size number of inputs that may be joined to this merge node
 */
class ZipSet[A] private (size: Int) extends FlexiMerge[Set[A]] {
  require(size >= 0)

  import FlexiMerge._

  val in = (0 until size).map { _ => createInputPort[A]() }.toVector

  def createMergeLogic() = new MergeLogic[Set[A]] {
    def initialState = State[ReadAllInputs](ReadAll(in)) { (ctx, _, inputs) =>
      ctx.emit(in.flatMap(port => inputs.get[A](port)).toSet)

      SameState[A]
    }

    def inputHandles(inputCount: Int) = {
      require(inputCount == size, s"ZipSet must have $size connected inputs, was $inputCount")

      in
    }

    override def initialCompletionHandling = eagerClose
  }

}

object ZipSet {

  def apply[A](size: Int) = {
    new ZipSet[A](size)
  }

}

// TODO: document
object GestureTokenizer {

  sealed trait TaggedAccelerometerValue

  case class GestureTag(name: String, matchProbability: Double, value: AccelerometerValue) extends TaggedAccelerometerValue

  case class ActivityTag(value: AccelerometerValue) extends TaggedAccelerometerValue

}

/**
 * Instances of this class recognise gestures and, based on this recognition, split or tokenize sensor streams. Filters
 * control which sensor locations are monitored and which sensor locations have their event streams tokenized.
 *
 * @param name       unique name identifying the gesture that we will recognise
 * @param monitoring collection of sensor locations in which we are to detect gestures
 * @param listeners  collection of locations whose sensor streams will be tokenized when we match a gesture
 */
// TODO: calling code needs to "normalise" sensor stream against the time dimension - i.e. here we assume that events occur with a known frequency (use `TickSource` as a driver for this?)
class GestureTokenizer(name: String, monitoring: Set[SensorDataSourceLocation], listeners: Set[SensorDataSourceLocation])(implicit config: Config) extends SVMClassifier {

  import FlowGraphImplicits._
  import GestureTokenizer._

  val frequency = config.getInt("classification.frequency")
  assert(frequency > 0)
  val threshold = config.getDouble(s"classification.gesture.$name.threshold")
  assert(0 <= threshold && threshold <= 1)
  val windowSize = config.getInt(s"classification.gesture.$name.size")
  assert(windowSize > 0)

  // NOTE: here we accept throwing an exception in loading R libSVM models (since this indicates a catastrophic configuration error!)
  val model = new SVMModelParser(name).model.get

  /**
   * Measures probability that sampled window is recognised as a gesture event.
   *
   * @param sample sampled data to be tested
   * @return       probability that a gesture was recognised in the sample window
   */
  def probabilityOfGestureEvent(sample: List[AccelerometerValue]): Double = {
    require(sample.length == windowSize)

    val data = DenseMatrix(sample.map(v => (v.x.toDouble, v.y.toDouble, v.z.toDouble)): _*)

    predict(model, data, taylor_radial_kernel()).positiveMatch
  }

  /**
   * Sensor based stream parser that sends tagging events (as a side effect) to a monitor whenever it recognises a gesture
   * event. If the sample window is detected as a gesture match, then data is tagged `GestureTag`, otherwise it is tagged
   * `ActivityTag`. Sensor stream messages are otherwise untouched by this function.
   *
   * @param sensor  sensor stream that is to be sampled for matching gesture events
   * @param monitor monitoring sink node to which we send data tagging events
   * @return        input sensor stream unchanged
   */
  def identifyGestureEvents(sensor: Source[AccelerometerValue], monitor: Sink[Transformation[AccelerometerValue, TaggedAccelerometerValue]])(implicit materializer: FlowMaterializer): Source[AccelerometerValue] = {
    sensor.transform(() => SamplingWindow[AccelerometerValue](windowSize) {
      case (sample: List[AccelerometerValue]) if sample.length == windowSize =>
        val matchProbability = probabilityOfGestureEvent(sample)

        if (matchProbability > threshold) {
          Source(List(Transformation[AccelerometerValue, TaggedAccelerometerValue](GestureTag(name, matchProbability, _)))).to(monitor).run
        } else {
          Source(List(Transformation[AccelerometerValue, TaggedAccelerometerValue](ActivityTag))).to(monitor).run
        }
    })
  }

  /**
   * Flowgraph that merges a collection of transformations into a single transformation.
   *
   * @param monitors set of transformation messages that we need to merge
   * @param out      node to which the merged transformation message will be sent
   * @param merge    function that defines how to merge transformations
   * @return         flowgraph that merges transformations into one transformation
   */
  def mergeTransformations[A, B](monitors: Set[Source[Transformation[A, B]]], out: Sink[Transformation[A, B]])(merge: PartialFunction[Set[Transformation[A, B]], Transformation[A, B]]) = FlowGraph { implicit builder =>
    val zip = ZipSet[Transformation[A, B]](monitors.size)

    for ((probe, index) <- monitors.zipWithIndex) {
      probe ~> zip.in(index)
    }
    zip.out ~> Flow[Set[Transformation[A, B]]].map(merge) ~> out
  }

  /**
   * Trigger action used to modulate sensor signals.
   *
   * @param action modulation action that is to be used to transform a sensor message
   */
  case class Transformation[A, B](action: A => B)

  /**
   * Flowgraph that transforms or modulates each sensor in a network of location tagged sensors. Messages on the `transform`
   * node determine how signals in the sensor net are modulated or transformed.
   *
   * @param sensorIn  map representing the location tagged sensor network
   * @param transform transformation signal used to modulate sensor data
   * @param sensorOut map representing the transformed or modulated signals in the location tagged sensor network
   * @return          flowgraph that modulates sensor net messages (from input to output) using synchronised messages on
   *                  the transform node
   */
  def modulateSensorNet[A, B, L](sensorIn: Map[L, Source[A]], transform: Source[Transformation[A, B]], sensorOut: Map[L, Sink[B]]) = FlowGraph { implicit builder =>
    require(sensorIn.keys == sensorOut.keys)

    val broadcast = Broadcast[Transformation[A, B]]

    transform ~> broadcast
    for ((location, sensor) <- sensorIn) {
      val zip = ZipWith[A, Transformation[A, B], B]((msg: A, transform: Transformation[A, B]) => transform.action(msg))

      sensor    ~> zip.left
      broadcast ~> zip.right
      zip.out   ~> sensorOut(location)
    }
  }

  // TODO: define aggregation strategies!

}
