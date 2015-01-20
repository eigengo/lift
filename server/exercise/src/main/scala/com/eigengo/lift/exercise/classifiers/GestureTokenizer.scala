package com.eigengo.lift.exercise

package classifiers

import akka.stream.scaladsl._
import akka.stream.stage.{PushStage, Context, Directive}
import breeze.linalg.DenseMatrix
import com.eigengo.lift.exercise.classifiers.svm.{SVMClassifier, SVMModelParser}
import com.typesafe.config.Config
import scala.collection.mutable

/**
 * Streaming stage that buffers events looking for a sample match. When a match is found, a specified action is executed.
 *
 * @param size             size of the internal buffer and so the sample window size
 * @param matchProbability probability that the (full) buffer elements are a match
 * @param threshold        threshold value at which matching probability is considered a match
 * @param action           action to be performed on detecting a match (matching probability is passed as a parameter to the function)
 */
class WindowMatcher[A](size: Int, matchProbability: List[A] => Double, threshold: Double)(action: Double => Unit) extends PushStage[A, A] {
  require(size > 0)
  require(0 <= threshold && threshold <= 1)

  private val buffer = mutable.Queue[A]()

  override def onPush(elem: A, ctx: Context[A]): Directive = {
    if (buffer.length == size) {
      // Buffer is full, so determine if the current sample window matches or not
      val emit = buffer.dequeue()
      buffer.enqueue(elem)
      val probability = matchProbability(buffer.toList)
      if (probability > threshold) {
        action(probability)
      }
      // Allow old events to propagate upstream
      ctx.push(emit)
    } else {
      // Buffer is not yet full, so keep consuming from our downstream
      buffer.enqueue(elem)
      ctx.pull()
    }
  }
}

object GestureTokenizer {

  /**
   * Marker trait identifying the collection of tokens that may be recognised
   */
  sealed trait Token

  /**
   * Token representing a recognised gesture.
   *
   * @param name     unique identifier - models name of classified gesture
   * @param location sensor location on which the gesture data was received
   * @param data     accelerometer data values received during the (recognised) gesture classification window
   */
  case class GestureToken private (name: String, location: SensorDataSourceLocation, data: List[AccelerometerValue]) extends Token

  /**
   * Token holding data from a (potential) activity or exercising window.
   *
   * @param location sensor location on which the accelerometer data was received
   * @param value    accelerometer data value that is to be part of the activity or exercising window
   */
  case class ActivityToken private (location: SensorDataSourceLocation, value: AccelerometerValue) extends Token

  /**
   * Internal event: measures probability that a gesture was recognised. Message is sent to aggregate sensor processor ]
   * so that they may either split the listening sensor streams or synchronously progress all the streams forward by an
   * event.
   *
   * @param probability probabilistic measure signifying the degree to which a gesture was recognised
   */
  case class GestureMatch(probability: Double)

}

/**
 * Instances of this class recognise gestures and, based on this recognition, split or tokenize sensor streams. Filters
 * control which sensor locations are monitored and which sensor locations have their event streams tokenized.
 *
 * @param name       unique name identifying the gesture that we will recognise
 * @param monitoring collection of sensor locations in which we are to detect gestures
 * @param listeners  collection of locations whose sensor streams will be tokenized when we match a gesture
 */
// TODO: calling code needs to "normalise" sensor stream against the time dimension - i.e. here we assume that events occur with a known frequency
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
   * Sensor based stream parser that sends split stream events (to listeners or subscribers) whenever it recognises a
   * gesture event.
   *
   * @param sensor sensor stream that is to be monitored for matching gesture events
   */
  def identifyGestureEvents(sensor: Source[AccelerometerValue]): Source[AccelerometerValue] = {
    sensor.transform(() => new WindowMatcher(windowSize, probabilityOfGestureEvent, threshold)( (probability: Double) =>
      ??? // TODO: send GestureMatch(probability) to subscribers or listeners
    ))
  }

  /**
   * Trigger action used to modulate sensor signals.
   *
   * @param action modulation action that is to be used to transform a sensor message
   */
  case class Transformation[A, B](action: A => B)

  /**
   * Flowgraph that monitors each sensor (in a network of location tagged sensors). Sensor messages are transformed using
   * a action or modulation function that is extracted from a "trigger" input node. Sensor message flow synchronously with
   * the messages on the "trigger" input node.
   *
   * @param sensorIn  map representing the location tagged sensor network
   * @param sensorOut sensor outputs transformed by "trigger" signal
   * @return          flowgraph that transforms sensor messages using "trigger" modulation signals
   */
  def sensorTransformation[A, B, L](sensorIn: Map[L, Source[A]], sensorOut: Map[L, Sink[B]]) = FlowGraph { implicit builder =>
    require(sensorIn.keys == sensorOut.keys)

    val transform = UndefinedSource[Transformation[A, B]] // Transformation signal used to modulate sensor data
    val broadcast = Broadcast[Transformation[A, B]]

    transform ~> broadcast
    for ((location, sensor) <- sensorIn) {
      val zip = ZipWith[A, Transformation[A, B], B]((msg: A, transform: Transformation[A, B]) => transform.action(msg))

      sensor    ~> zip.left
      broadcast ~> zip.right
      zip.out   ~> sensorOut(location)
    }
  }

}
