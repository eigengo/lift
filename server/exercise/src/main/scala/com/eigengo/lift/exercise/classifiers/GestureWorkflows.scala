package com.eigengo.lift.exercise

package classifiers

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
class SamplingWindow[A] private (size: Int, action: List[A] => Unit) extends PushStage[A, A] {
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
      // Streaming stage is shutting down, so we ensure that all buffer elements are flushed prior to finishing
      if (buffer.isEmpty) {
        // Buffer is empty, so we simply finish
        ctx.finish()
      } else if (buffer.length == 1) {
        // One element in the buffer, so push it and finish
        ctx.pushAndFinish(buffer.dequeue())
      } else {
        // Multiple elements are in the buffer, so reduce its size and eventually we will finish
        ctx.push(buffer.dequeue())
      }
    }
  }

  override def onUpstreamFinish(ctx: Context[A]): TerminationDirective = {
    ctx.absorbTermination()
  }
}

object SamplingWindow {

  def apply[A](size: Int)(action: List[A] => Unit): SamplingWindow[A] = {
    new SamplingWindow(size, action)
  }

}

/**
 * Streaming stage that buffers events (the sample window). Once the buffer has filled, a strategy function is applied
 * to determine the elements to be emitted. Emitted values are (typically) either a grouping of the elements making up
 * the sample or single sample elements.
 *
 * @param size     size of the internal buffer and so the sampling window size
 * @param strategy when buffer is full, sample window strategy that determines the elements to be emitted
 */
class GroupBySample[A] private (size: Int, strategy: List[A] => GroupBySample.GroupValue[A]) extends PushStage[A, GroupBySample.GroupValue[A]] {
  require(size > 0)

  import GroupBySample._

  private val buffer = mutable.Queue[A]()

  private def applyStrategy(ctx: Context[GroupValue[A]]): Directive = {
    strategy(buffer.toList) match {
      case value @ BlobValue(sample) =>
        // Aggregated sample to be emitted
        buffer.drop(sample.length)
        ctx.push(value)

      case value: SingleValue[A] =>
        // Single value sample is to be emitted
        buffer.dequeue()
        ctx.push(value)
    }
  }

  override def onPush(elem: A, ctx: Context[GroupValue[A]]): Directive = {
    if (!ctx.isFinishing) {
      if (buffer.length == size) {
        // Buffer is full, so apply our strategy to determine emit behaviour
        applyStrategy(ctx)
      } else {
        // Buffer is not yet full, so keep consuming from our upstream
        buffer.enqueue(elem)
        ctx.pull()
      }
    } else {
      // Streaming stage is shutting down, so we ensure that all buffer elements are flushed prior to finishing
      if (buffer.isEmpty) {
        // Buffer is empty, so we simply finish
        ctx.finish()
      } else if (buffer.length == 1) {
        // One element in the buffer, so push it and finish
        ctx.pushAndFinish(SingleValue(buffer.dequeue()))
      } else if (buffer.length == size) {
        // Buffer is full, so apply our strategy to determine emit behaviour
        applyStrategy(ctx)
      } else {
        // Multiple elements are in the buffer (and it isn't full), so reduce its size and eventually we will finish
        ctx.push(SingleValue(buffer.dequeue()))
      }
    }
  }

  override def onUpstreamFinish(ctx: Context[GroupValue[A]]): TerminationDirective = {
    ctx.absorbTermination()
  }
}

object GroupBySample {

  /**
   * Trait that allows data streams to be grouped into common values (c.f. `BlobValue`). Data values that can not be
   * grouped are "marked" with `SingleValue`.
   */
  sealed trait GroupValue[A]
  case class BlobValue[A](value: List[A]) extends GroupValue[A]
  case class SingleValue[A](value: A) extends GroupValue[A]

  def apply[A](size: Int)(strategy: List[A] => GroupValue[A]) = {
    new GroupBySample[A](size, strategy)
  }

}

/**
 * Flowgraph merge node that expects a message on each of its inputs. The collection of input messages is then outputted.
 * This merge node can be thought of as a generalised Zip or ZipWith node.
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

object GestureTagging {

  /**
   * Trait that allows stream data to be tagged and classified. Data can be recognised as being the start of a gesture
   * (c.f. `GestureTag`) or as being part of an activity window (c.f. `ActivityTag`).
   */
  sealed trait TaggedValue[A]
  case class GestureTag[A](name: String, matchProbability: Double, value: A) extends TaggedValue[A]
  case class ActivityTag[A](value: A) extends TaggedValue[A]

}

/**
 * Trait that implements reactive stream components that can:
 *
 *   * [identifyGestureEvents] tap into sensor streams and trigger transformation events whenever a sample window is classified as a gesture
 *   * [mergeTransformations]  merge collections of transformation events into a single transformation event
 *   * [modulateSensorNet]     modulate the signals (e.g. by tagging them) in a network of sensors using a transformation signal
 *   * [gestureCollector]      split tagged sensor streams using highest probability gesture matches
 */
// TODO: calling code needs to "normalise" sensor stream against the time dimension - i.e. here we assume that events occur with a known frequency (use `TickSource` as a driver for this?)
trait GestureWorkflows extends SVMClassifier {

  import FlowGraphImplicits._
  import GestureTagging._
  import GroupBySample._

  def name: String
  def config: Config

  val frequency = config.getInt("classification.frequency")
  assert(frequency > 0)
  val threshold = config.getDouble(s"classification.gesture.$name.threshold")
  assert(0 <= threshold && threshold <= 1)
  val windowSize = config.getInt(s"classification.gesture.$name.size")
  assert(windowSize > 0)

  // NOTE: here we accept throwing an exception in loading R libSVM models (since this indicates a catastrophic configuration error!)
  private lazy val model = new SVMModelParser(name)(config).model.get

  /**
   * Trigger action used to modulate sensor signals.
   *
   * @param action modulation action that is to be used to transform a sensor message
   */
  case class Transformation[A, B](action: A => B)

  /**
   * Measures probability that sampled window is recognised as a gesture event.
   *
   * @param sample sampled data to be tested
   * @return       probability that a gesture was recognised in the sample window
   */
  private def probabilityOfGestureEvent(sample: List[AccelerometerValue]): Double = {
    require(sample.length == windowSize)

    val data = DenseMatrix(sample.map(v => (v.x.toDouble, v.y.toDouble, v.z.toDouble)): _*)

    predict(model, data, taylor_radial_kernel()).positiveMatch
  }

  /**
   * Flowgraph that taps the in-out stream and, if a gesture is recognised, sends a tagged message to the `tap` sink.
   */
  class IdentifyGestureEvents {
    val in = UndefinedSource[AccelerometerValue]
    val out = UndefinedSink[AccelerometerValue]
    val tap = UndefinedSink[Transformation[AccelerometerValue, TaggedValue[AccelerometerValue]]]

    val graph = PartialFlowGraph { implicit builder =>

      in ~> Flow[AccelerometerValue].transform(() => SamplingWindow[AccelerometerValue](windowSize) {
        case (sample: List[AccelerometerValue]) if sample.length == windowSize =>
          val matchProbability = probabilityOfGestureEvent(sample)

          if (matchProbability > threshold) {
            Source.single(Transformation[AccelerometerValue, TaggedValue[AccelerometerValue]](GestureTag[AccelerometerValue](name, matchProbability, _)))
          } else {
            Source.single(Transformation[AccelerometerValue, TaggedValue[AccelerometerValue]](ActivityTag[AccelerometerValue]))
          } ~> tap
      }) ~> out
    }
  }

  object IdentifyGestureEvents {
    def apply() = new IdentifyGestureEvents()
  }

  /**
   * Flowgraph that merges (via a user supplied function) a collection of input sources into a single output sink.
   *
   * @param size number of input sources that we are to bundle and merge
   */
  class MergeTransformations[A, B](size: Int, merge: Set[Transformation[A, B]] => Transformation[A, B]) {
    require(size > 0)

    val in = (0 until size).map(_ => UndefinedSource[Transformation[A, B]])
    val out = UndefinedSink[Transformation[A, B]]

    val graph = PartialFlowGraph { implicit builder =>
      val zip = ZipSet[Transformation[A, B]](in.size)

      for ((probe, index) <- in.zipWithIndex) {
        probe ~> zip.in(index)
      }
      zip.out ~> Flow[Set[Transformation[A, B]]].map(merge) ~> out
    }
  }

  object MergeTransformations {
    def apply[A, B](size: Int)(merge: Set[Transformation[A, B]] => Transformation[A, B]) = {
      new MergeTransformations[A, B](size, merge)
    }
  }

  /**
   * Flowgraph that modulates all sensors in a network of location tagged sensors. Messages on the `transform` source
   * determine how signals in the sensor net are modulated or transformed.
   *
   * @param locations set of locations that make up the sensor network
   */
  class ModulateSensorNet[A, B, L](locations: Set[L]) {
    val in = locations.map(loc => (loc, UndefinedSource[A])).toMap
    val transform = UndefinedSource[Transformation[A, B]]
    val out = locations.map(loc => (loc, UndefinedSink[B])).toMap

    val graph = PartialFlowGraph { implicit builder =>
      val broadcast = Broadcast[Transformation[A, B]]

      transform ~> broadcast
      for ((location, sensor) <- in) {
        val zip = ZipWith[A, Transformation[A, B], B]((msg: A, transform: Transformation[A, B]) => transform.action(msg))

        sensor    ~> zip.left
        broadcast ~> zip.right
        zip.out   ~> out(location)
      }
    }
  }

  object ModulateSensorNet {
    def apply[A, B, L](locations: Set[L]) = new ModulateSensorNet[A, B, L](locations)
  }

  /**
   * Flowgraph that groups messages on the input source.
   */
  class GestureGrouping[A] {
    val in = UndefinedSource[TaggedValue[A]]
    val out = UndefinedSink[GroupValue[TaggedValue[A]]]

    val graph = PartialFlowGraph { implicit builder =>
      in ~> Flow[TaggedValue[A]].transform(() => GroupBySample[TaggedValue[A]](2 * windowSize) { (sample: List[TaggedValue[A]]) =>
        require(sample.length == 2 * windowSize)

        val gestures = sample.take(windowSize).takeWhile {
          case elem: GestureTag[A] =>
            true

          case _ =>
            false
        }.map(_.asInstanceOf[GestureTag[A]])

        if (gestures.isEmpty) {
          // Sample prefix contains no recognisable gestures
          SingleValue(sample.head)
        } else {
          // Sample prefix contains at least one recognisable gesture - we need to check if the head message has highest recognition probability
          if (gestures.head.matchProbability >= gestures.map(_.matchProbability).max) {
            // Head of sample window is our best match - emit it
            BlobValue(gestures.slice(0, windowSize))
          } else {
            // Sample window is not yet at the best match
            SingleValue(gestures.head)
          }
        }
      }) ~> out
    }
  }

  object GestureGrouping {
    def apply[A]() = new GestureGrouping[A]()
  }

  /**
   * Flowgraph that monitors the `inputLocations` sensoir network for recognisable gestures. When gestures are detected,
   * messages on the `outputLocations` sensor network are tagged and grouped.
   *
   * @param inputLocations  locations that make up the input sensor network
   * @param outputLocations locations that make up the output sensor network
   */
  class GestureClassificationWorkflow[L](inputLocations: Set[L], outputLocations: Set[L]) {
    require(inputLocations.nonEmpty)
    require(outputLocations.nonEmpty)

    // Tapped sensors - monitored for recognisable gestures
    val inputTap = inputLocations.map(loc => (loc, UndefinedSource[AccelerometerValue])).toMap
    val outputTap = inputLocations.map(loc => (loc, UndefinedSink[AccelerometerValue])).toMap
    // Grouped sensors - event streams are tagged and grouped
    val groupedIn = outputLocations.map(loc => (loc, UndefinedSource[AccelerometerValue])).toMap
    val groupedOutput = outputLocations.map(loc => (loc, UndefinedSink[GroupValue[TaggedValue[AccelerometerValue]]])).toMap

    val graph = PartialFlowGraph { implicit builder =>
      val modulate = ModulateSensorNet[AccelerometerValue, TaggedValue[AccelerometerValue], L](outputLocations)
      val merge = MergeTransformations[AccelerometerValue, TaggedValue[AccelerometerValue]](inputLocations.size) { (obs: Set[Transformation[AccelerometerValue, TaggedValue[AccelerometerValue]]]) =>
        ??? // FIXME:
      }

      // Wire in tapped sensors
      for ((loc, index) <- inputLocations.zipWithIndex) {
        val identify = IdentifyGestureEvents()

        builder.importPartialFlowGraph(identify.graph)
        builder.connect(inputTap(loc), Flow[AccelerometerValue], identify.in)
        builder.connect(identify.out, Flow[AccelerometerValue], outputTap(loc))
        builder.connect(identify.tap, Flow[Transformation[AccelerometerValue, TaggedValue[AccelerometerValue]]], merge.in(index))
      }

      // Wire in modulation
      builder.importPartialFlowGraph(modulate.graph)
      builder.connect(merge.out, Flow[Transformation[AccelerometerValue, TaggedValue[AccelerometerValue]]], modulate.transform)

      // Wire in grouping
      for (loc <- outputLocations) {
        val group = GestureGrouping[AccelerometerValue]()

        builder.importPartialFlowGraph(group.graph)
        builder.connect(modulate.out(loc), Flow[TaggedValue[AccelerometerValue]], group.in)
        builder.connect(groupedIn(loc), Flow[AccelerometerValue], modulate.in(loc))
        builder.connect(group.out, Flow[GroupValue[TaggedValue[AccelerometerValue]]], groupedOutput(loc))
      }
    }
  }

  object GestureClassificationWorkflow {
    def apply[L](inputLocations: Set[L], outputLocations: Set[L]) = new GestureClassificationWorkflow[L](inputLocations, outputLocations)
  }

}
