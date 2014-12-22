package com.eigengo.lift.analysis.exercise.rt


import scalaz.\/

trait ExerciseCodecs {

  object ExerciseCodec extends MessageDecoder[Exercise] with MessageEncoder[Exercise] {

    override def decode(data: Array[Byte]): String \/ Exercise = {
      ???
    }

    override def encode(value: Exercise): String \/ Array[Byte] =
      ???
  }

}
