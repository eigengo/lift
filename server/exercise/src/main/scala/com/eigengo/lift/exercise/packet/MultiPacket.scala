package com.eigengo.lift.exercise.packet

import com.eigengo.lift.exercise.{SensorDataSourceLocation, SensorDataWithLocation}
import scodec.bits.BitVector

/**
 * Raw bits received from a location. For example wrist watch.
 *
 * @param location
 * @param data
 */
case class RawSensorData(location: SensorDataSourceLocation, data: BitVector)

/**
 * Raw sensor data from locations are gathered by a device (phone) and sent together to the server
 * This class represents all these data
 * Multiple devices with the same location will be both as separate element in the Seq
 *
 * @param rawSensorData raw sensor data from each device
 */
case class MultiPacket(rawSensorData: Seq[RawSensorData])