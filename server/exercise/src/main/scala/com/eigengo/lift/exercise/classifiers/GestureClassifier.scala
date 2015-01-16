package com.eigengo.lift.exercise

package classifiers

import akka.stream.scaladsl._
import com.typesafe.config.Config

trait GestureClassifier {
  this: { def config: Config } =>

  def name: String

  val windowSize = config.getInt(s"classification.gesture.$name.size")

  def isGestureEvent(sample: List[AccelerometerData]): Boolean = {
    require(sample.length == windowSize)

    ??? // TODO:
  }

  val workflow = Flow[SensorDataWithLocation]
    .filter {
      case SensorDataWithLocation(location, data: List[AccelerometerData]) =>
        true

      case _ =>
        false
    }
    .groupBy {
      case SensorDataWithLocation(location, _) =>
        location
    }
    .map {
      case (location, sensorSource) =>
        sensorSource
          .mapConcat {
            case SensorDataWithLocation(_, data: List[AccelerometerData]) =>
              data
          }
          .scan[List[AccelerometerData]](List()) {
            case (window, event: AccelerometerData) =>
              val newWindow = window :+ event
              if (newWindow.length > windowSize) {
                newWindow.tail
              } else {
                newWindow
              }
          }
          .filter(_.length == windowSize)
          .splitWhen(isGestureEvent)
    }

}
