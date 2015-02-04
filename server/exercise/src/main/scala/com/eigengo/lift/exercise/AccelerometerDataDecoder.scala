package com.eigengo.lift.exercise

import scodec.bits.{ByteOrdering, BitVector}

import scalaz.\/

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
 *     uint8_t type;                   // 1 (0xad)
 *     uint8_t count;                  // 2
 *     uint8_t samples_per_second;     // 3
 *     uint8_t sample_size;            // 4
 *     uint8_t __padding;              // 5
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
object AccelerometerDataDecoder extends SensorDataDecoder[AccelerometerData] {
  private implicit val _ = scalaz.Monoid.instance[String](_ + _, "")
  private val header = ConstantCodecPrimitive(BitVector(0xad))

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

      override val bits: Long = 40

      override def decode(buffer: BitVector): \/[String, (BitVector, (Int, Int))] = {
        // S, C, Const
        for {
          (b0, _)     ← unsigned16.decode(buffer) // sample size + padding
          (b1, sps)   ← unsigned8.decode(b0)      // samplesPerSecond
          (b2, count) ← unsigned8.decode(b1)      // count
          (b3, _)     ← header.decode(b2)         // type
        } yield (b3, (sps, count))
      }
    }
  )

  override def supports(bits: BitVector): Boolean = {
    val x = header.decode(bits)
    x.isRight
  }

  override def decode(bits: BitVector): \/[String, (BitVector, AccelerometerData)] = for {
    (body, (sps, count)) ← packedGfsHeader.decode(bits)
    (rest, avs)          ← packedAccelerometerData.decode[List](body, count)
  } yield rest → AccelerometerData(sps, avs)
}
