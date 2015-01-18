package com.eigengo.lift.exercise

package classifiers

import akka.stream.scaladsl._
import com.typesafe.config.Config

object GestureTokenizer {

  /**
   * Marker trait respresenting collection of tokens that may be recognised
   */
  sealed trait Token

  /**
   * Token representing a recognised gesture.
   *
   * @param name     unique identifier - models names of classified gesture
   * @param location sensor location for which the gesture was recognised
   * @param data     data that was recognised
   */
  case class GestureToken private (name: String, location: SensorDataSourceLocation, data: List[AccelerometerData]) extends Token

  /**
   * Token representing a (potential) exercising window.
   *
   * @param location sensor location for which the gesture was recognised
   * @param data     data that was recognised
   */
  case class ExerciseToken private (location: SensorDataSourceLocation, data: List[AccelerometerData]) extends Token

}

/**
 * Instances of this class recognise gestures and, based on this recognition, split or tokenize sensor streams. A filter
 * controls which sensor locations have their event streams tokenized.
 *
 * @param name           unique name identifying the gesture that we recognise
 * @param locationFilter collection of locations for which we are to (gesture) tokenize sensor streams
 * @param config
 */
class GestureTokenizer(name: String, locationFilter: Set[SensorDataSourceLocation])(implicit config: Config) {

  import GestureTokenizer._

  val windowSize = config.getInt(s"classification.gesture.$name.size")
  assert(windowSize > 0)

  def isGestureEvent(sample: List[AccelerometerData]): Boolean = {
    require(sample.length == windowSize)

    ???
  }

  def parseGestures(location: SensorDataSourceLocation, data: List[AccelerometerData]): (List[Token], List[AccelerometerData]) = {
    val (parsed, unparsed) = data
      .sliding(windowSize)
      .span {
        case window if window.length == windowSize =>
          if (isGestureEvent(window)) {
            false
          } else {
            true
          }

        case window =>
          true
      }
    if (unparsed.nonEmpty) {
      val (gesture, partialData) = unparsed.flatten.toList.splitAt(parsed.length)
      val (remainingTokens, remainingData) = parseGestures(location, partialData)

      (List(ExerciseToken(location, parsed.flatten.toList), GestureToken(name, location, gesture)) ++ remainingTokens, remainingData)
    } else {
      (List(), data)
    }
  }

  val flow: Flow[SensorDataWithLocation[AccelerometerData], List[Token]] =
    Flow[SensorDataWithLocation[AccelerometerData]]
      .filter(data => locationFilter.contains(data.location))
      .groupBy(_.location)
      .map { case (location, sensorSource) =>
        location ->
          sensorSource
            .scan[(List[Token], List[AccelerometerData])]((List(), List())) {
              case ((parsed, unparsed), sensor) =>
                val (gestures, remaining) = parseGestures(location, unparsed ++ sensor.data)
                (parsed ++ gestures, remaining)
            }
            .mapConcat(_._1)
      }

}
