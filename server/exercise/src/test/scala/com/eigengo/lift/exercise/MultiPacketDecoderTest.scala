package com.eigengo.lift.exercise

import java.nio.ByteBuffer

import org.scalatest.{Matchers, FlatSpec}

import scala.util.Random
import scalaz.\/-

class MultiPacketDecoderTest extends FlatSpec with Matchers {

  def payload(size: Short, sloc: Byte, content: Array[Byte]): Array[Byte] = {
    val sizeh = (size >> 8).toByte
    val sizel = (size & 0xff00 >> 8).toByte
    Array(sizeh, sizel, sloc) ++ content
  }

  def generate(slocs: List[Byte], size: Byte ⇒ Short, content: Byte ⇒ Array[Byte]): Array[Byte] = {
    val header: Array[Byte] = Array.apply(0xca.toByte, 0xb0.toByte, slocs.size.toByte)
    val payloads = slocs.map(sloc ⇒ payload(size(sloc), sloc, content(sloc)))
    payloads.foldLeft(header)(_ ++ _)
  }

  def randomSize(sloc: Byte): Short = Random.nextInt(65536).toShort
  def constSize(s: Short)(sloc: Byte): Short = s
  def constContent(b: Byte*)(sloc: Byte): Array[Byte] = b.toArray

  "Single valid packet" should "decode" in {
    val \/-(x) = MultiPacketDecoder.decode(ByteBuffer.wrap(generate(List(0x01), constSize(1), constContent(0x00))))
    x.packets(0).payload.getByte(0) should be(0)
  }

}
