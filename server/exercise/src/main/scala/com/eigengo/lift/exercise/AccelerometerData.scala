package com.eigengo.lift.exercise

import scodec.bits.{BitVector, ByteOrdering}

import scala.annotation.tailrec
import scalaz.\/

/**
 * Accelerometer data groups ``values`` at the given ``samplingRate``
 * @param samplingRate the sampling rate in Hz
 * @param values the values
 */
case class AccelerometerData(samplingRate: Int, values: List[AccelerometerValue]) extends SensorData

/**
 * Accelerometer data
 * @param x the x
 * @param y the y
 * @param z the z
 */
case class AccelerometerValue(x: Int, y: Int, z: Int)
