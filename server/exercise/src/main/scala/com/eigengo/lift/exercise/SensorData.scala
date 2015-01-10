package com.eigengo.lift.exercise

import scodec.bits.BitVector

import scalaz.\/

/**
 * Sensor data marker trait
 */
trait SensorData

/**
 * Decodes some sensor data from the bits
 * @tparam A the type of sensor data
 */
trait SensorDataDecoder[A] {
  
  def supports(bits: BitVector): Boolean
  
  def decode(bits: BitVector): String \/ (BitVector, A)

}

trait RootSensorDataDecoder[A] extends SensorDataDecoder[A] {

  final def decodeAll(bits: BitVector): String \/ A = {
    decode(bits).flatMap {
      case (BitVector.empty, v) ⇒ \/.right(v)
      case (_, _)               ⇒ \/.left("Undecoded bits")
    }
  }

}

object SensorDataDecoder {
  private implicit val _ = scalaz.Monoid.instance[String](_ + _, "")

  def apply(decoders: Seq[SensorDataDecoder[SensorData]]): RootSensorDataDecoder[List[SensorData]] =
    new RootSensorDataDecoder[List[SensorData]] {
      override def supports(bits: BitVector): Boolean = true

      override def decode(bits: BitVector): String \/ (BitVector, List[SensorData]) = {
        def decode0(bits: BitVector, acc: List[SensorData]): String \/ (BitVector, List[SensorData]) = {
          decoders.find(_.supports(bits)).map { decoder ⇒
            decoder.decode(bits).flatMap {
              case (bv, sd) ⇒ decode0(bv, sd :: acc)
            }
          }.getOrElse(\/.left("No decoder"))
        }

        if (bits.isEmpty) \/.left("Empty bits")
        else decode0(bits, Nil)
      }
    }

}
