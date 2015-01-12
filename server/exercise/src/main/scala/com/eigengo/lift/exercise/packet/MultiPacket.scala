package com.eigengo.lift.exercise.packet

import com.eigengo.lift.exercise.SensorDataWithLocation

case class MultiPacket(sensorData: Seq[SensorDataWithLocation])