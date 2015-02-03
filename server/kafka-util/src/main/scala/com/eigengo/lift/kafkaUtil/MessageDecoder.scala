package com.eigengo.lift.kafkaUtil

import com.eigengo.lift.kafkaUtil.MessagePayload.Payload

import scalaz.{\/, \/-, -\/}

trait MessageDecoder[A] {
  def decode(data: Payload): String \/ A
}

