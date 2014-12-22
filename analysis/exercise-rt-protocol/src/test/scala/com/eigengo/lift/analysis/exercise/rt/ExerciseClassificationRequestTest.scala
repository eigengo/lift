package com.eigengo.lift.analysis.exercise.rt

import java.util

import com.eigengo.lift.analysis.exercise.rt.ExerciseClassificationProtocol.{ExerciseClassificationRequest, Payload, Train}
import org.scalacheck.Properties

import scalaz.\/-

object ExerciseClassificationRequestTest extends Properties("Exercise classification") with JavaSerializationCodecs {
  import org.scalacheck.Prop._
  val encoder = implicitly[MessageEncoder[ExerciseClassificationRequest]]
  val decoder = implicitly[MessageDecoder[ExerciseClassificationRequest]]

  property("Encoding and decoding") = forAll { (name: String, int: Double, payload: Payload) ⇒
    val t = Train(payload, Exercise(name, int))
    val \/-(Train(p2, ex2)) = for {
      encoded ← encoder.encode(t)
      decoded ← decoder.decode(encoded)
    } yield decoded

    t.exercise == ex2 && util.Arrays.equals(t.payload, p2)
  }

}
