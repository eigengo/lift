package com.eigengo.lift.exercise

import scodec.bits.BitVector

case class PacketWithLocation(sourceLocation: SensorDataSourceLocation, payload: BitVector)

case class MultiPacket(timestamp: Long, packets: List[PacketWithLocation]) {
  def withNewPacket(packet: PacketWithLocation): MultiPacket = copy(packets = packets :+ packet)
}

object MultiPacket {
  def single(timestamp: Long)(pwl: PacketWithLocation): MultiPacket = MultiPacket(timestamp, List(pwl))
}
