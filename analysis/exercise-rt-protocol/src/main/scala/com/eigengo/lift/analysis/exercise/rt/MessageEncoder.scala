package com.eigengo.lift.analysis.exercise.rt

import scalaz.\/

trait MessageEncoder[A] {

  def encode(value: A): String \/ Array[Byte]

}
