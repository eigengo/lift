package com.eigengo.lift.exercise

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