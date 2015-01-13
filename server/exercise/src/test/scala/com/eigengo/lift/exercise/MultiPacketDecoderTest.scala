package com.eigengo.lift.exercise

import java.nio.ByteBuffer

import org.scalatest.{FlatSpec, Matchers}

import scalaz.{-\/, \/-}

class MultiPacketDecoderTest extends FlatSpec with Matchers {

  /// writes payload of the given size at sloc and content.
  /// note that we pass size and content explicitly to allow us to construct
  /// badly formed payload
  private def payload(size: Int, sloc: Byte, content: Array[Byte]): Array[Byte] = {
    val sizeh = (size >> 8).toByte
    val sizel = (size & 0xff00 >> 8).toByte
    Array(sizeh, sizel, sloc) ++ content
  }

  /// generate incoming message for the given slocs, sizes and content
  private def generate(slocs: List[Byte], size: Byte ⇒ Int, content: (Byte, Int) ⇒ Array[Byte]): Array[Byte] = {
    val header: Array[Byte] = Array.apply(0xca.toByte, 0xb0.toByte, slocs.size.toByte)
    val payloads = slocs.map { sloc ⇒ val s = size(sloc); payload(s, sloc, content(sloc, s)) }
    payloads.foldLeft(header)(_ ++ _)
  }

  private def constSize(s: Int)(sloc: Byte): Int = s
  private def constContent(b: Byte)(sloc: Byte, size: Int): Array[Byte] = Array.fill(size)(b)
  private def badContent(sloc: Byte, size: Int): Array[Byte] = Array.empty

  "Single valid packet" should "decode" in {
    val \/-(x) = MultiPacketDecoder.decode(ByteBuffer.wrap(generate(List(0x01), constSize(1), constContent(0x00))))
    x.packets(0).payload.getByte(0) should be(0)
  }

  "Multiple valid, max size packets" should "decode" in {
    val in = generate(List(0x01, 0x02, 0x03, 0x04, 0x7f), constSize(65535), constContent(0x7f))
    val \/-(x) = MultiPacketDecoder.decode(ByteBuffer.wrap(in))
    x.packets.size should be (5)
    x.packets.foreach(_.payload.getByte(0) should be(0x7f))
  }

  "Very badly malformed input" should "fail decoding" in {
    val -\/("No viable input: size < 7.") = MultiPacketDecoder.decode(ByteBuffer.wrap(Array.empty))
    val -\/("Incorrect header. Expected -13648, got 0.") = MultiPacketDecoder.decode(ByteBuffer.wrap(Array.fill(7)(0)))
    val -\/("No content.") = MultiPacketDecoder.decode(ByteBuffer.wrap(Array[Byte](0xca.toByte, 0xb0.toByte, 0x00, 0x00, 0x00, 0x00, 0x00)))
  }

  "Malformed content" should "fail decoding" in {
    val in = generate(List(0x01, 0x02, 0x03, 0x04, 0x7f), constSize(65535), badContent)
    val -\/("Incomplete or truncated input. (65535 bytes payload of packet 0.)") = MultiPacketDecoder.decode(ByteBuffer.wrap(in))
  }

}
