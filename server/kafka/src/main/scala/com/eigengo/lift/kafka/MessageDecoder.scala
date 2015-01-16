package com.eigengo.lift.kafka

import scalaz.\/
import MessagePayload.Payload

trait MessageDecoder[A] {
  def decode(data: Payload): String \/ A
}

