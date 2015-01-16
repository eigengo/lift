package com.eigengo.lift.kafka

import scalaz.\/
import MessagePayload.Payload

trait MessageEncoder[A] {
  def encode(value: A): String \/ Payload
}
