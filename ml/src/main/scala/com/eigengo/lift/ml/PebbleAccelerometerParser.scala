package com.eigengo.lift.ml

import scodec.Codec
import scodec.bits.BitVector

import scala.collection.mutable.ListBuffer
import scalaz.\/-

trait PebbleAccelerometerParser {
  import scodec.codecs._

  private type ZYX = (Int, Int, Int)
  private type CSU = (Int, Int, Unit)

  private implicit val packedAccelerometerData: Codec[ZYX] = new FixedSizeReversedCodec(40, {
      ignore(1) :~>: ("z" | int(13)) :: ("y" | int(13)) :: ("x" | int(13))
  }).as[ZYX]

  private implicit val packedGfsHeader: Codec[CSU] = new FixedSizeReversedCodec(40, {
    ("samplesPerSecond" | int8) :: ("count" | int16) :: constant(BitVector(0xfe, 0xfc))
  }).as[CSU]

  def parse(bits: BitVector): List[AccelerometerData] = {
    implicit val _ = scalaz.Monoid.instance[String](_ + _, "")
    var b = bits
    val result = ListBuffer[AccelerometerData]()
    while (b.nonEmpty) {
      val r = Codec.decode[CSU](b).flatMap {
        case (body, (samplesPerSecond, count, _)) =>
          Codec.decodeCollect[List, ZYX](packedAccelerometerData, Some(count))(body).flatMap {
            case (rest, zyxs) =>
              val avs = zyxs.map { case (z, y, x) => AccelerometerValue(x, y, z)}
              \/-(rest, AccelerometerData(samplesPerSecond, avs))
          }
      }

      r.fold(_ => b = b.drop(1), { case (rest, ad) => b = rest; result += ad })
    }

    result.toList
  }

}

object PebbleAccelerometerParser extends PebbleAccelerometerParser