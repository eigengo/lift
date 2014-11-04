package com.eigengo.pe

import scodec.Codec
import scodec.bits.BitVector

import scala.annotation.tailrec

/**
 * Accelerometer data groups ``values`` at the given ``samplingRate``
 * @param samplingRate the sampling rate in Hz
 * @param values the values
 */
case class AccelerometerData(samplingRate: Int, values: List[AccelerometerValue])

/**
 * Accelerometer data
 * @param x the x
 * @param y the y
 * @param z the z
 */
case class AccelerometerValue(x: Int, y: Int, z: Int)

/**
 * Contains decoders for the stream of paced values in a stream constructed from
 *
 * {{{
 * #define GFS_HEADER_TYPE (uint16_t)0xfefc
 *
 * /**
 * * 5 B in header
 * */
 * struct __attribute__((__packed__)) gfs_header {
 *     uint16_t type;
 *     uint16_t count;
 *     uint8_t samples_per_second;
 * };
 *
 * /**
 * * Packed 5 B of the accelerometer values
 * */
 * struct __attribute__((__packed__)) gfs_packed_accel_data {
 *     int16_t x_val : 13;
 *     int16_t y_val : 13;
 *     int16_t z_val : 13;
 * };
 * }}}
 */
object AccelerometerData {
  import scodec.codecs._

  private type ZYX = (Int, Int, Int)
  private type CSU = (Int, Int, Unit)

  private implicit val packedAccelerometerData: Codec[ZYX] = new FixedSizeReversedCodec(40, {
    ignore(1) :~>: ("z" | int(13)) :: ("y" | int(13)) :: ("x" | int(13))
  }).as[ZYX]

  private implicit val packedGfsHeader: Codec[CSU] = new FixedSizeReversedCodec(40, {
    ("samplesPerSecond" | int8) :: ("count" | int16) :: constant(BitVector(0xfe, 0xfc))
  }).as[CSU]

  private def decode(bits: BitVector): (BitVector, List[AccelerometerData]) = {
    implicit val _ = scalaz.Monoid.instance[String](_ + _, "")
    val result = for {
      (body, (samplesPerSecond, count, _)) <- Codec.decode[CSU](bits)
      (rest, zyxs) <- Codec.decodeCollect[List, ZYX](packedAccelerometerData, Some(count))(body)
      avs = zyxs.map { case (z, y, x) => AccelerometerValue(x, y, z) }
    } yield (rest, AccelerometerData(samplesPerSecond, avs))

    result.fold(_ => (bits, Nil), { case (bits2, ad) => (bits2, List(ad)) })
  }

  /**
   * Decodes as much as possible from ``bits``, appending the values to ``ads``.
   * @param bits the input bit stream
   * @param ads the "current" list of ``AccelerometerData``
   * @return the remaining bits and decoded ``AccelerometerData``
   */
  @tailrec
  final def decodeAll(bits: BitVector, ads: List[AccelerometerData]): (BitVector, List[AccelerometerData]) = {
    decode(bits) match {
      // Parsed all we could, nothing remains
      case (BitVector.empty, ads2) => (BitVector.empty, ads ++ ads2)
      // Parsed all we could, but exactly `bits` remain => we did not get any further.
      // Repeated recursion will not solve anything.
      case (`bits`, ads2) => (bits, ads ++ ads2)
      // Still something left to parse
      case (neb, ads2) => decodeAll(neb, ads ++ ads2)
    }
  }

}
