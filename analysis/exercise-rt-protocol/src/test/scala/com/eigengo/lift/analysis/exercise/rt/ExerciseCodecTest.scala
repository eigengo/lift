package com.eigengo.lift.analysis.exercise.rt

import org.scalacheck.Properties

import scalaz.\/-

object ExerciseCodecTest extends Properties("Encoder") with JavaSerializationCodecs {
  import org.scalacheck.Prop.forAll
  val encoder = implicitly[MessageEncoder[Exercise]]
  val decoder = implicitly[MessageDecoder[Exercise]]

  property("Encode and decode") = forAll { (name: String, intensity: Double) ⇒
    val e = Exercise(name, intensity)
    val \/-(e2) = for {
      encoded ← encoder.encode(e)
      decoded ← decoder.decode(encoded)
    } yield decoded

    e == e2
  }

}
