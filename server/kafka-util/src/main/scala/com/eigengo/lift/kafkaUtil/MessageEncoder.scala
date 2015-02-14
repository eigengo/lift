package com.eigengo.lift.kafkaUtil

import com.eigengo.lift.kafkaUtil.MessagePayload.Payload

import scalaz.{\/, \/-, -\/}

trait MessageEncoder[A] {
  def encode(value: A): String \/ Payload
}
