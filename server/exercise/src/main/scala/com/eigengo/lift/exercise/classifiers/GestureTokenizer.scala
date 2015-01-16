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
   * @param location sensor location for which the gesture was recognised
   * @param data     data that was recognised
   */
  case class GestureToken private (location: SensorDataSourceLocation, data: List[AccelerometerData]) extends Token

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

  def parseGestures(data: List[AccelerometerData]): (List[Token], List[AccelerometerData]) = {
    data.zipWithIndex
      .sliding(windowSize)
      .span {
        case window if window.length == windowSize =>
          ???

        case window =>
          true
      }
  }

  val workflow = Flow[SensorDataWithLocation[AccelerometerData]]
    .filter(data => locationFilter.contains(data.location))
    .groupBy(_.location)
    .map {
      case (location, sensorSource) =>
        (location,
         sensorSource
           .scan[(List[Token], List[AccelerometerData])]((List(), List())) {
             case ((parsed, unparsed), sensor) =>
               val (gestures, remaining) = parseGestures(unparsed ++ sensor.data)
               (parsed ++ gestures, remaining)
           }
        )
    }

}
