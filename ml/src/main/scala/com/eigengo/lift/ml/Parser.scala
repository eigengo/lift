package com.eigengo.lift.ml

import scodec.{Decoder, CodecAsAux, Codec}
import scodec.bits.BitVector

import scalaz.\/

trait PebbleAccelerometerParser {
  import scodec.codecs._

  private type ZYX = (Int, Int, Int)

  private case class Gfs(header: GfsHeader, data: List[AccelerometerValue])
  private case class GfsHeader(count: Int, samplesPerSecond: Int)

  private implicit val packedAccelerometerData: Codec[ZYX] = fixedSizeBits(40, {
      ignore(7) :~>: ("z" | int(11)) :: ("y" | int(11)) :: ("x" | int(11))
  }).as[ZYX]

  private implicit val packedGfsHeader: Codec[GfsHeader] = fixedSizeBytes(5, {
    constant(BitVector(0xff, 0xf7)) :~>: ("count" | int16) :: ("samplesPerSecond" | int8)
  }).as[GfsHeader]

  def parsePackedAccelerometerData(bytes: BitVector): \/[String, (BitVector, AccelerometerValue)] = {
    val rev = bytes.reverseByteOrder
    Codec.decode[ZYX](rev).map {
      case (bv, (z, y, x)) => (bv, AccelerometerValue(x, y, z))
    }
    // FF, FF, FF, FF, 01 is pad->x_val = -1; pad->y_val = -1; pad->z_val = -1;
    // 01, 08, 40, 00, 00 is pad->x_val = +1; pad->y_val = +1; pad->z_val = +1;
    // 8C, C0, 39, FA, 00 is pad->x_val = 140; pad->y_val = -200; pad->z_val = 1000;


    // x  8B from (0): 0-7 + 3B from (1): 0-2
    // y  5B from (1): 3-7 + 6B from (2): 0-5
    // z  2B from (2): 6-7 + 8B from (3): 0-7 + 1B from (3) 0
  }

  def parse(bits: BitVector): List[AccelerometerData] = {
    val rev = bits.reverseByteOrder
    implicit val _ = scalaz.Monoid.instance[String](_ + _, "")

    def iter(b: BitVector, ads: List[AccelerometerData]): \/[String, (BitVector, AccelerometerData)] = {
      for {
        (body, header) <- Codec.decode[GfsHeader](b)
        (rest, zyxs) <- Codec.decodeCollect[List, ZYX](packedAccelerometerData, Some(header.count))(body)
        avs = zyxs.map { case (z, y, x) => AccelerometerValue(x, y, z)}
        if rest.nonEmpty
        (x, y) <- iter(rest, AccelerometerData(header.samplesPerSecond, avs) :: ads)
      } yield (x, y)
    }

    iter(rev, Nil).toList.map(_._2)
  }

}

object PebbleAccelerometerParser extends PebbleAccelerometerParser