package com.eigengo.lift.ml

import scodec.Codec
import scodec.bits.BitVector

import scala.collection.mutable.ListBuffer
import scalaz.{\/-, \/}

trait PebbleAccelerometerParser {
  import scodec.codecs._

  private type ZYX = (Int, Int, Int)
  private type CSU = (Int, Int, Unit)

  private implicit val packedAccelerometerData: Codec[ZYX] = new FixedSizeReversedCodec(40, {
      ignore(7) :~>: ("z" | int(11)) :: ("y" | int(11)) :: ("x" | int(11))
  }).as[ZYX]

  private implicit val packedGfsHeader: Codec[CSU] = new FixedSizeReversedCodec(40, {
    ("samplesPerSecond" | int8) :: ("count" | int16) :: constant(BitVector(0xfe, 0xf7))
  }).as[CSU]

  def parsePackedAccelerometerData(bytes: BitVector): \/[String, (BitVector, AccelerometerValue)] = {
    Codec.decode[ZYX](bytes).map {
      case (bv, (z, y, x)) => (bv, AccelerometerValue(x, y, z))
    }
    // FF, FF, FF, FF, 01 is pad->x_val = -1; pad->y_val = -1; pad->z_val = -1;
    // 01, 08, 40, 00, 00 is pad->x_val = +1; pad->y_val = +1; pad->z_val = +1;
    // 8C, C0, 39, FA, 00 is pad->x_val = 140; pad->y_val = -200; pad->z_val = 1000;


    // x  8B from (0): 0-7 + 3B from (1): 0-2
    // y  5B from (1): 3-7 + 6B from (2): 0-5
    // z  2B from (2): 6-7 + 8B from (3): 0-7 + 1B from (3) 0
  }

  def parseGfsHeader(bytes: BitVector) = {
    Codec.decode[CSU](bytes)
  }

  def parse(bits: BitVector): List[AccelerometerData] = {
    implicit val _ = scalaz.Monoid.instance[String](_ + _, "")
    var b = bits
    val result = ListBuffer[AccelerometerData]()
    while (b.nonEmpty) {
      val r = for {
        (body, (samplesPerSecond, count, _))  <- Codec.decode[CSU](b)
        (rest, zyxs) <- Codec.decodeCollect[List, ZYX](packedAccelerometerData, Some(count))(body)
        avs = zyxs.map { case (z, y, x) => AccelerometerValue(x, y, z)}
      } yield (rest, AccelerometerData(samplesPerSecond, avs))

      r.fold(_ => (), { case (rest, ad) => b = rest; result += ad })
    }

    result.toList
  }

}

object PebbleAccelerometerParser extends PebbleAccelerometerParser