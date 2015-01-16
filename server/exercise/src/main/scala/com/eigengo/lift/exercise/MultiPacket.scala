package com.eigengo.lift.exercise

import scodec.bits.BitVector

case class PacketWithLocation(sourceLocation: SensorDataSourceLocation, payload: BitVector)

case class MultiPacket(packets: List[PacketWithLocation]) {
  def withNewPacket(packet: PacketWithLocation): MultiPacket = copy(packets = packets :+ packet)
}

object MultiPacket {
  def single(pwl: PacketWithLocation): MultiPacket = MultiPacket(List(pwl))
}
