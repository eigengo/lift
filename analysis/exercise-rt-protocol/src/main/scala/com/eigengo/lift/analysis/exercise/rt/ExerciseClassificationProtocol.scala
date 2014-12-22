package com.eigengo.lift.analysis.exercise.rt

import scalaz.\/

object ExerciseClassificationProtocol {

  type Payload = Array[Byte]

  sealed trait ExerciseClassificationRequest {
    def payload: Payload

    def decodedPayload[A : MessageDecoder]: String \/ A = implicitly[MessageDecoder[A]].decode(payload)
  }

  case class Train(payload: Payload, exercise: Exercise) extends ExerciseClassificationRequest
  case class Classify(id: String, payload: Payload) extends ExerciseClassificationRequest

}
