package com.eigengo.lift.exercise

/**
 * Rotation data groups ``values`` at the given ``samplingRate``
 *
 * @param samplingRate the sampling rate in Hz
 * @param values the values
 */
case class RotationData(samplingRate: Int, values: List[RotationValue]) extends SensorData

/**
 * Rotation rate data in mrad/s
 * @param x the x
 * @param y the y
 * @param z the z
 */
case class RotationValue(x: Int, y: Int, z: Int) extends SensorValue

/**
 * Rotation data decoder
 */
object RotationDataDecoder extends AccelerationDataLikeDecoder(0xbd.toByte, RotationValue, RotationData)
