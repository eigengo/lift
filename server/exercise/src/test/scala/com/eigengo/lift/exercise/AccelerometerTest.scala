package com.eigengo.lift.exercise

import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.{ByteOrdering, BitVector}

import scalaz.\/-

/**
 * Tests that the accelerometer data can be decoded from a stream constructed from
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
 *
 * The data arrives in big endian encoded-stream, but with reverse byte order in the
 * "packets". And thus,
 *
 * {{{
 * fcfe 0300 64 -> count = 3, samplesPerSecond = 100 (Hz)
 * ffff ffff 01 -> x =  -1, y =  -1, z =  127
 * 0000 0000 01 -> x =   0, y =   0, z =   64
 * 7801 4ac0 73 -> x = 376, y = 592, z = -784
 * fcfe 0100 64 -> count = 1, samplesPerSecond = 100 (Hz)
 * 7801 4ac0 73 -> x =  376, y = 592, z = -784
 * }}}
 */
class AccelerometerTest extends FlatSpec with Matchers {

  "Decoder" should "decode values" in {
    val bits = BitVector(
      0xfc, 0xfe, 0x03, 0x00, 0x64,
      0xff, 0xff, 0xff, 0xff, 0x01,
      0x00, 0x00, 0x00, 0x00, 0x01,
      0x78, 0x01, 0x4a, 0xc0, 0x73,
      0xfc, 0xfe, 0x01, 0x00, 0x64,
      0x78, 0x01, 0x4a, 0xc0, 0x73)

    val (BitVector.empty, ads) = AccelerometerData.decodeAll(bits, Nil)
    ads(0).values should contain allOf(AccelerometerValue(-1, -1, 127), AccelerometerValue(0, 0, 64), AccelerometerValue(376, 592, -784))
    ads(1).values should contain (AccelerometerValue(376, 592, -784))
  }

  "Decoder" should "decode training file" in {
    val bv = BitVector.fromInputStream(getClass.getResourceAsStream("/training/arm3.dat"))
    val (BitVector.empty, ads) = AccelerometerData.decodeAll(bv, Nil)
    println(ads)
  }

  "Decoder" should "manipulate bits" in {
    val pad = BitVector(0x78, 0x01, 0x4a, 0xc0, 0x73).reverseByteOrder

    val \/-((_, z)) = IntCodecPrimitive(13, signed = true, ByteOrdering.BigEndian).decode(pad.drop(1))
    val \/-((_, y)) = IntCodecPrimitive(13, signed = true, ByteOrdering.BigEndian).decode(pad.drop(14))
    val \/-((_, x)) = IntCodecPrimitive(13, signed = true, ByteOrdering.BigEndian).decode(pad.drop(27))
    z should be(-784)
    y should be(592)
    x should be(376)
  }
}
