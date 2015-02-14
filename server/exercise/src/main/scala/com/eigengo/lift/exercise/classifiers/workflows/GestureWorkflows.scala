package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.scaladsl._
import breeze.linalg.DenseMatrix
import com.eigengo.lift.exercise.AccelerometerValue
import com.eigengo.lift.exercise.classifiers.svm.{SVMClassifier, SVMModelParser}
import com.typesafe.config.Config

/**
 * Trait that implements reactive stream components that can:
 *
 *   * [IdentifyGestureEvents] tap into sensor streams and generate fact events whenever a sample window is classified as a gesture
 *   * [MergeSignals]          merge collections of fact signals into a single fact event
 *   * [ModulateSensorNet]     modulate the signals (e.g. by binding them with facts) in a network of sensors
 */
trait GestureWorkflows extends SVMClassifier {

  import ClassificationAssertions._

  def name: String
  def config: Config

  lazy val frequency = {
    val value = config.getInt("classification.frequency")
    assert(value > 0)
    value
  }
  lazy val threshold = {
    val value = config.getDouble(s"classification.gesture.$name.threshold")
    assert(0 <= value && value <= 1)
    value
  }
  lazy val windowSize = {
    val value = config.getInt(s"classification.gesture.$name.size")
    assert(value > 0)
    value
  }

  // NOTE: here we accept throwing an exception in loading R libSVM models (since this indicates a catastrophic configuration error!)
  private lazy val model = new SVMModelParser(name)(config).model.get

  /**
   * Measures probability that sampled window is recognised as a gesture event.
   *
   * @param sample sampled data to be tested
   * @return       probability that a gesture was recognised in the sample window
   */
  private def probabilityOfGestureEvent(sample: List[AccelerometerValue]): Double = {
    require(sample.length == windowSize)

    val data = DenseMatrix(sample.map(v => (v.x.toDouble, v.y.toDouble, v.z.toDouble)): _*)

    predict(model, data, taylorRadialKernel()).positiveMatch
  }

  /**
   * Flowgraph that taps the in stream and, if a gesture is recognised, sends a `Fact` message to the `out` sink.
   */
  def IdentifyGestureEvents(): Flow[AccelerometerValue, Option[Fact]] =
    Flow[AccelerometerValue]
      .transform(() => SlidingWindow[AccelerometerValue](windowSize))
      .map { (sample: List[AccelerometerValue]) =>
        if (sample.length == windowSize) {
          // Saturated windows may be classified
          val matchProbability = probabilityOfGestureEvent(sample)

          if (matchProbability >= threshold) {
            Some(Gesture(name, threshold))
          } else {
            Some(NegGesture(name, threshold))
          }
        } else {
          // Truncated windows are never classified (these typically occur when the stream closes)
          None
        }
      }

}
