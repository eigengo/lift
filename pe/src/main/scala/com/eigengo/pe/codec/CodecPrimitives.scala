package com.eigengo.pe.codec

import java.nio.ByteOrder

import scodec.bits.{BitVector, ByteVector}

import scala.language.higherKinds
import scalaz.{\/-, -\/, \/}

trait CodecPrimitive[A] {
  type Err = String
  def bits: Long
  def decode(buffer: BitVector): \/[String, (BitVector, A)]

  final def decodeCollect[F[_]](buffer: BitVector, limit: Int)(implicit cbf: collection.generic.CanBuildFrom[F[A], A, F[A]]): \/[String, (BitVector, F[A])] = {
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
  override def decode(buffer: BitVector): \/[String, (BitVector, A)] = {
    buffer.acquire(codec.bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(b) ⇒
        codec.decode(b.reverseByteOrder) match {
          case e @ -\/(_) ⇒ e
          case \/-((rest, res)) ⇒ \/-((buffer.drop(codec.bits), res))
        }
    }
  }
}

case class IgnoreCodecPrimitive(bits: Long) extends CodecPrimitive[Unit] {
  override def decode(buffer: BitVector): \/[String, (BitVector, Unit)] =
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(b) ⇒ \/.right((buffer.drop(bits), ()))
    }
}

case class IntCodecPrimitive(bits: Long, signed: Boolean, ordering: ByteOrder) extends CodecPrimitive[Int] {

  require(bits > 0 && bits <= (if (signed) 32 else 31), "bits must be in range [1, 32] for signed and [1, 31] for unsigned")

  override def decode(buffer: BitVector): \/[String, (BitVector, Int)] = {
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(b) ⇒
        val mod = bits % 8
        var result = 0
        ordering match {
          case ByteOrder.BIG_ENDIAN ⇒
            @annotation.tailrec
            def go(bv: ByteVector): Unit =
              if (bv.nonEmpty) {
                result = (result << 8) | (0x0ff & bv.head)
                go(bv.tail)
              }
            go(b.toByteVector)
          case ByteOrder.LITTLE_ENDIAN ⇒
            @annotation.tailrec
            def go(bv: ByteVector, i: Int): Unit =
              if (bv.nonEmpty) {
                result = result | ((0x0ff & bv.head) << (8 * i))
                go(bv.tail, i + 1)
              }
            go(b.toByteVector, 0)
        }
        if (mod != 0) result = result >>> (8 - mod)
        // Sign extend if necessary
        if (signed && bits != 32 && ((1 << (bits - 1)) & result) != 0) {
          val toShift = 32 - bits
          result = (result << toShift) >> toShift
        }
        \/.right((buffer.drop(bits), result))
    }
  }
}

case class ConstantCodecPrimitive(constant: BitVector) extends CodecPrimitive[Unit] {
  lazy val bits = constant.size

  override def decode(buffer: BitVector): \/[String, (BitVector, Unit)] =
    buffer.acquire(bits) match {
      case Left(e) ⇒ \/.left(e)
      case Right(`constant`) ⇒ \/.right((buffer.drop(bits), ()))
      case Right(b) ⇒ \/.left(s"expected constant $constant but got $b")
    }

}
