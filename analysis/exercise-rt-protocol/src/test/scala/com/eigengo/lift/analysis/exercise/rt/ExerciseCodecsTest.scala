package com.eigengo.lift.analysis.exercise.rt

import org.scalacheck.Properties

import scalaz.\/-

object ExerciseCodecsTest extends Properties("Encoder") with ExerciseCodecs {
  import org.scalacheck.Prop.forAll

  property("Encode and decode") = forAll { (name: String, intensity: Double) ⇒
    val e = Exercise(name, intensity)
    val \/-(e2) = for {
      encoded ← ExerciseCodec.encode(e)
      decoded ← ExerciseCodec.decode(encoded)
    } yield decoded

    e == e2
  }

}
