package com.eigengo.lift.exercise

/**
 * Sensor data marker trait
 */
trait SensorData

/**
 * Container for sensor data at a given location. This grouping means that it is possible
 * to receive multiple sensor data from a single location. A watch (on the wrist) may be capable
 * of sending accelerometer data, HR data and oxygenation data; a mobile (near the waist) may send
 * accelerometer, gyroscope and GPS data.
 *
 * @param location the location
 * @param data the data
 */
case class SensorDataWithLocation(location: SensorDataSourceLocation, data: List[SensorData])

