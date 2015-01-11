package com.eigengo.lift.exercise

import scodec.bits.BitVector

import scalaz.\/

/**
 * Sensor data marker trait
 */
trait SensorData

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

/**
 * Decodes some sensor data from the bits
 * @tparam A the type of sensor data
 */
trait SensorDataDecoder[+A] {

  /**
   * Determines if this decoder can decode the given ``bits``
   * @param bits the bits to be decoded
   * @return ``true`` if ``#decode`` is likely to succeed
   */
  def supports(bits: BitVector): Boolean

  /**
   * Decodes the ``bits`` into a tuple containing undecoded bits and the
   * decoded value on the right, or the error on the left
   * @param bits the bits
   * @return either error or (undecoded bits, A)
   */
  def decode(bits: BitVector): String \/ (BitVector, A)

}

/**
 * Root decoder adds ``decodeAll`` method to ensure that no bits are left undecoded. It
 * is also not covariant in A: there should only be one ``RootSensorDataDecoder``, typically
 * constructed by calling {{RootSensorDataDecoder.apply}}.
 *
 * @tparam A the type of sensor data
 */
trait RootSensorDataDecoder[A] extends SensorDataDecoder[A] {

  /**
   * Decodes all values from ``bits``, ensuring that there is nothing left after decoding
   * @param bits the bits to be decoded
   * @return the error or A
   */
  final def decodeAll(bits: BitVector): String \/ A = {
    decode(bits).flatMap {
      case (BitVector.empty, v) ⇒ \/.right(v)
      case (_, _)               ⇒ \/.left("Undecoded bits")
    }
  }

}

/**
 * Sensor data decoder companion, which constructs a ``RootSensorDataDecoder[List[SensorData]]`` from
 * a sequence of ``SensorDataDecoder[SensorData]``. Typically, you'll keep the returned value in a
 * variable:
 *
 * {{{
 *   val decoder = RootSensorDataDecoder(AccelerometerDataDecoder, HeartRateDataDecoder, GeolocationDataDecoder)
 *   ...
 *
 *   decoder.decodeAll(bits)
 * }}}
 */
object RootSensorDataDecoder {
  private implicit val _ = scalaz.Monoid.instance[String](_ + _, "")

  def apply(decoders: SensorDataDecoder[SensorData]*): RootSensorDataDecoder[List[SensorData]] =
    new RootSensorDataDecoder[List[SensorData]] {
      override def supports(bits: BitVector): Boolean = true

      override def decode(bits: BitVector): String \/ (BitVector, List[SensorData]) = {
        def decode0(bits: BitVector, acc: List[SensorData]): String \/ (BitVector, List[SensorData]) = {
          decoders.find(_.supports(bits)).map { decoder ⇒
            decoder.decode(bits).flatMap {
              // decoded all there is to be decoded
              case (BitVector.empty, sd) ⇒ \/.right(BitVector.empty → (acc :+ sd))
              // more bits to be decoded, but same as bits; no need to try further
              case (`bits`, _)           ⇒ \/.left("Undecoded bits")
              // more bits to be decoded
              case (neb, sd)             ⇒ decode0(neb, acc :+ sd)
            }
          }.getOrElse(\/.left("No decoder"))
        }

        if (bits.isEmpty) \/.left("Empty bits")
        else decode0(bits, Nil)
      }
    }

}
