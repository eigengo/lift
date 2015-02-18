package com.eigengo.lift.exercise

import scodec.bits.{BitVector, ByteOrdering}

import scala.language.higherKinds
import scalaz._

trait CodecPrimitive[A] {
  type Err = String
  type Decoded[R] = \/[Err, (BitVector, R)]
  def bits: Long

  def decode(buffer: BitVector): Decoded[A]

  final def decode[F[_]](buffer: BitVector, limit: Int)(implicit cbf: collection.generic.CanBuildFrom[F[A], A, F[A]]): Decoded[F[A]] = {
    val builder = cbf()
    var remaining = buffer
    var count = 0
    var error: Option[Err] = None
    while (count < limit && remaining.nonEmpty) {
      decode(remaining) match {
        case \/-((rest, value)) =>
          builder += value
          count += 1
          remaining = rest
        case -\/(err) =>
          error = Some(err)
          remaining = BitVector.empty
      }
    }

    error.map(\/.left).getOrElse(\/.right(remaining, builder.result()))
  }
}

class ReverseByteOrderCodecPrimitive[A](codec: CodecPrimitive[A]) extends CodecPrimitive[A] {
  override lazy val bits: Long = codec.bits
  override def decode(buffer: BitVector): Decoded[A] = {
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(b) ⇒
        codec.decode(b.reverseByteOrder) match {
          case e @ -\/(_) ⇒ e
          case \/-((_, res)) ⇒ \/-((buffer.drop(bits), res))
        }
    }
  }
}

case class IgnoreCodecPrimitive(bits: Long) extends CodecPrimitive[Unit] {
  override def decode(buffer: BitVector): Decoded[Unit] =
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(b) ⇒ \/.right((buffer.drop(bits), ()))
    }
}

case class IntCodecPrimitive(bits: Long, signed: Boolean, ordering: ByteOrdering) extends CodecPrimitive[Int] {

  require(bits > 0 && bits <= (if (signed) 32 else 31), "bits must be in range [1, 32] for signed and [1, 31] for unsigned")

  override def decode(buffer: BitVector): Decoded[Int] = {
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(b) ⇒ \/.right((buffer.drop(bits), b.toInt(signed, ordering)))
    }
  }
}

case class ConstantCodecPrimitive(constant: BitVector) extends CodecPrimitive[Unit] {
  lazy val bits = constant.size

  override def decode(buffer: BitVector): Decoded[Unit] =
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(`constant`) ⇒ \/.right((buffer.drop(bits), ()))
      case Right(b) ⇒ \/.left(s"expected constant $constant but got $b")
    }

}
