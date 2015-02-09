package com.eigengo.lift.exercise

/**
 * Sensor data marker trait
 */
trait SensorData {
  def samplingRate: Int
  def values: List[SensorValue]
}

/**
 * Sensor value marker trait
 */
trait SensorValue

/**
 * Location of the sensor on the human body. Regardless of what the sensor measures, we
 * are interested in knowing its location on the body.
 *
 * Consider accelerometer data received from a sensor on the wrist and waist. If the user is
 * doing (triceps) dips, we expect the data from the wrist sensor to be farily flat, but the
 * data from the waist sensor to show that the user's body is moving up and down.
 *
 * Other types of sensor data are the same—assuming the sensors are just as capable of reliably
 * measuring it—regardless of where the sensor is located. Heart rate is the same whether it is
 * measured by a chest strap or by a watch.
 */
sealed trait SensorDataSourceLocation
/// sensor on a wrist: typically a smart watch
case object SensorDataSourceLocationWrist extends SensorDataSourceLocation
/// sensor around user's waist: e.g. mobile in a pocket
case object SensorDataSourceLocationWaist extends SensorDataSourceLocation
/// sensor near the user's foot: e.g. shoe sensor
case object SensorDataSourceLocationFoot extends SensorDataSourceLocation
/// sensor near the user's chest: typically a HR belt
case object SensorDataSourceLocationChest extends SensorDataSourceLocation
/// sensor with unknown location or where the location does not make a difference
case object SensorDataSourceLocationAny extends SensorDataSourceLocation

object Sensor {
  val sourceLocations = Set(
    SensorDataSourceLocationWrist,
    SensorDataSourceLocationWaist,
    SensorDataSourceLocationFoot,
    SensorDataSourceLocationChest,
    SensorDataSourceLocationAny
  )
}

/**
 * Used to model a full sensor network of locations that may transmit data to us. Instances of the case class represent
 * sensor signals at a given point in time.
 */
case class SensorNet(wrist: SensorData, waist: SensorData, foot: SensorData, chest: SensorData, unknown: SensorData) {
  val toMap = Map[SensorDataSourceLocation, SensorData](
    SensorDataSourceLocationWrist -> wrist,
    SensorDataSourceLocationWaist -> waist,
    SensorDataSourceLocationFoot -> foot,
    SensorDataSourceLocationChest -> chest,
    SensorDataSourceLocationAny -> unknown
  )
}

object SensorNet {
  def apply(sensorMap: Map[SensorDataSourceLocation, SensorData]) =
    new SensorNet(
      sensorMap(SensorDataSourceLocationWrist),
      sensorMap(SensorDataSourceLocationWaist),
      sensorMap(SensorDataSourceLocationFoot),
      sensorMap(SensorDataSourceLocationChest),
      sensorMap(SensorDataSourceLocationAny)
    )
}

/**
 * Location or column slice through a sensor network.
 */
case class SensorNetValue(wrist: SensorValue, waist: SensorValue, foot: SensorValue, chest: SensorValue, unknown: SensorValue) {
  val toMap = Map[SensorDataSourceLocation, SensorValue](
    SensorDataSourceLocationWrist -> wrist,
    SensorDataSourceLocationWaist -> waist,
    SensorDataSourceLocationFoot -> foot,
    SensorDataSourceLocationChest -> chest,
    SensorDataSourceLocationAny -> unknown
  )
}

object SensorNetValue {
  def apply(sensorMap: Map[SensorDataSourceLocation, SensorValue]) =
    new SensorNetValue(
      sensorMap(SensorDataSourceLocationWrist),
      sensorMap(SensorDataSourceLocationWaist),
      sensorMap(SensorDataSourceLocationFoot),
      sensorMap(SensorDataSourceLocationChest),
      sensorMap(SensorDataSourceLocationAny)
    )
}

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
