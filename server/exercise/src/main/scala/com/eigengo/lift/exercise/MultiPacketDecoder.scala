package com.eigengo.lift.exercise

import java.nio.{ByteOrder, ByteBuffer}

import scodec.bits.{BitVector, ByteOrdering}

import scalaz.\/

/**
 * The multi packet message follows a simple structure
 *
 * {{{
 *    header: UInt16     = 0xcab0 // 2
 *    count: Byte        = ...    // 3
 *    ===
 *    size0: UInt16      = ...    // 5
 *    sloc0: Byte        = ...    // 6
 *    data0: Array[Byte] = ...
 *    size1: UInt16      = ...
 *    sloc1: Byte        = ...
 *    data1: Array[Byte] = ...
 *    ...
 *    sizen: UInt16      = ...
 *    slocn: Byte        = ...
 *    datan: Array[Byte] = ...
 * }}}
 */
object MultiPacketDecoder {
  private val header = 0xcab0.toShort
  private val uint16 = IntCodecPrimitive(16, signed = false, ByteOrdering.BigEndian)

  def decode(input: ByteBuffer): String \/ MultiPacket = {
    val bebb = input.order(ByteOrder.BIG_ENDIAN)
    if (input.limit() < 7) \/.left("No viable input: size < 7.")
    else {
      val inputHeader = input.getShort
      if (inputHeader != header) \/.left(s"Incorrect header. Expected $header, got $inputHeader.")
      else {
        val count = bebb.get()
        if (count == 0) \/.left("No content")
        else {
          var position = bebb.position()
          val (h :: t) = (0 until count).toList.map { _ ⇒
            val size = bebb.getShort
            val sloc = bebb.get()
            val buf = bebb.slice().limit(size).asInstanceOf[ByteBuffer]
            position = position + size
            bebb.position(position)

            \/.right(PacketWithLocation(SensorDataSourceLocationAny, BitVector(buf)))
          }

          t.foldLeft(h.map(MultiPacket.apply))((r, b) ⇒ r.flatMap(mp ⇒ b.map(mp.withNewPacket)))
        }
      }
    }
  }

}
