package com.eigengo.lift.ml

case class AccelerometerData(samplingRate: Int, values: List[AccelerometerValue])

case class AccelerometerValue(x: Int, y: Int, z: Int)
