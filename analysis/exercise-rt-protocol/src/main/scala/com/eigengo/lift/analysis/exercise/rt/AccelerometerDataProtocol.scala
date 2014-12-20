package com.eigengo.lift.analysis.exercise.rt

import com.eigengo.lift.analysis.exercise.AccelerometerData

object AccelerometerDataProtocol {

  case class TrainAccelerometerData(ad: AccelerometerData, exercise: String, intensity: Double)

  case class PredictAccelerometerData(ad: AccelerometerData)

}
