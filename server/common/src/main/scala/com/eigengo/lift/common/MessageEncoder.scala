package com.eigengo.lift.common

import com.eigengo.lift.common.MessagePayload.Payload

import scalaz.\/

trait MessageEncoder[A] {
  def encode(value: A): String \/ Payload
}
