package com.eigengo.lift.analysis.exercise.rt.accelerometerdata

import com.eigengo.lift.analysis.exercise.rt.{MessageDecoder, AccelerometerValue, AccelerometerData}
import scodec.bits.{BitVector, ByteOrdering}

import scala.annotation.tailrec
import scalaz.\/

trait AccelerometerDataDecoder {

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
  implicit object AccelerometerDataMessageDecoder extends MessageDecoder[AccelerometerData] {
    private implicit val _ = scalaz.Monoid.instance[String](_ + _, "")

    private val packedAccelerometerData = new ReverseByteOrderCodecPrimitive(
      new CodecPrimitive[AccelerometerValue] {
        val ignore1 = IgnoreCodecPrimitive(1)
        val int13 = IntCodecPrimitive(13, signed = true, ByteOrdering.BigEndian)

        override val bits: Long = 40

        override def decode(buffer: BitVector): \/[String, (BitVector, AccelerometerValue)] = {
          for {
            (b1, _) ← ignore1.decode(buffer)
            (b2, z) ← int13.decode(b1)
            (b3, y) ← int13.decode(b2)
            (b4, x) ← int13.decode(b3)
          } yield (b4, AccelerometerValue(x, y, z))
        }
      }
    )

    private val packedGfsHeader = new ReverseByteOrderCodecPrimitive(
      new CodecPrimitive[(Int, Int)] {
        val unsigned8 = IntCodecPrimitive(8, signed = false, ByteOrdering.BigEndian)
        val unsigned16 = IntCodecPrimitive(16, signed = false, ByteOrdering.BigEndian)
        val header = ConstantCodecPrimitive(BitVector(0xfe, 0xfc))

        override val bits: Long = 40

        override def decode(buffer: BitVector): \/[String, (BitVector, (Int, Int))] = {
          // S, C, Const
          for {
            (b1, sps) ← unsigned8.decode(buffer)
            (b2, count) ← unsigned16.decode(b1)
            (b3, _) ← header.decode(b2)
          } yield (b3, (sps, count))
        }
      }
    )

    private def decode(bits: BitVector): (BitVector, List[AccelerometerData]) = {
      val result = for {
        (body, (sps, count)) ← packedGfsHeader.decode(bits)
        (rest, avs) ← packedAccelerometerData.decode[List](body, count)
      } yield (rest, AccelerometerData(sps, avs))

      result.fold(_ => (bits, Nil), { case (bits2, ad) => (bits2, List(ad))})
    }

    /**
     * Decodes as much as possible from ``bits``, appending the values to ``ads``.
     * @param bits the input bit stream
     * @param ads the "current" list of ``AccelerometerData``
     * @return the remaining bits and decoded ``AccelerometerData``
     */
    @tailrec
    private def decodeAll(bits: BitVector, ads: List[AccelerometerData]): (BitVector, List[AccelerometerData]) = {
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

    override def decode(data: Array[Byte]): String \/ AccelerometerData = {
      decodeAll(BitVector(data), Nil) match {
        case (BitVector.empty, h::t) ⇒
          if (t.forall(_.samplingRate == h.samplingRate)) {
            val ad = t.foldLeft(h) { (r, ad) ⇒ r.copy(values = r.values ++ ad.values)}
            \/.right(ad)
          } else {
            \/.left("Unmatched sampling rates")
          }
        case (_, _) ⇒ \/.left("Incomplete packet")
      }
    }
  }

}
