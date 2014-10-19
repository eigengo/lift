package com.eigengo.lift.ml

import scodec.Codec
import scodec.bits.BitVector

import scalaz.\/

trait PebbleAccelerometerParser {
  import scodec.codecs._

  private type ZYX = (Int, Int, Int)

  case class GfsHeader(tpe: Int, count: Int, samplesPerSecond: Int)

  private implicit val packedAccelerometerData: Codec[ZYX] = fixedSizeBits(33, {
      ("z" | int(11)) :: ("y" | int(11)) :: ("x" | int(11))
  }).as[ZYX]

  private implicit val packedGfsHeader: Codec[GfsHeader] = fixedSizeBytes(5, {
    "type" | int16 :: ("count" | int16) :: ("samplesPerSecond" | int8)
  }).as[GfsHeader]

  //private implicit val pebbleCodec = listOfN(packedGfsHeader.map(_.count), packedAccelerometerData)

  def parsePackedAccelerometerData(bytes: BitVector): \/[String, (BitVector, AccelerometerData)] = {
    val rev = bytes.reverseByteOrder.drop(7)
    Codec.decode[ZYX](rev).map {
      case (bv, (z, y, x)) => (bv, AccelerometerData(x, y, z))
    }
    // FF, FF, FF, FF, 01 is pad->x_val = -1; pad->y_val = -1; pad->z_val = -1;
    // 01, 08, 40, 00, 00 is pad->x_val = +1; pad->y_val = +1; pad->z_val = +1;
    // 8C, C0, 39, FA, 00 is pad->x_val = 140; pad->y_val = -200; pad->z_val = 1000;


    // x  8B from (0): 0-7 + 3B from (1): 0-2
    // y  5B from (1): 3-7 + 6B from (2): 0-5
    // z  2B from (2): 6-7 + 8B from (3): 0-7 + 1B from (3) 0
  }

}

object PebbleAccelerometerParser extends PebbleAccelerometerParser
