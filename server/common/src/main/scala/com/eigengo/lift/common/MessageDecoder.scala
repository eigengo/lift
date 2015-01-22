package com.eigengo.lift.common

import com.eigengo.lift.common.MessagePayload.Payload

import scalaz.\/

trait MessageDecoder[A] {
  def decode(data: Payload): String \/ A
}

