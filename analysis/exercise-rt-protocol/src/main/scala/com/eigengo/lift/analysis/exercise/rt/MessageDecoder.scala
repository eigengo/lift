package com.eigengo.lift.analysis.exercise.rt

import scalaz.\/

trait MessageDecoder[A] {

  def decode(data: Array[Byte]): String \/ A

}

