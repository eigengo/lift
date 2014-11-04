package com.eigengo.pe.exercise

import com.eigengo.pe.LiftMarshallers
import scodec.bits.BitVector
import spray.routing.HttpService

trait UserExerciseProcessorService extends HttpService with LiftMarshallers {
  import com.eigengo.pe.exercise.UserExerciseProtocol._
  def exerciseProcessor = actorRefFactory.actorSelection("/user/userExerciseProcessor")

  val userExerciseProcessorRoute =
    path("exercise") {
      post {
        handleWith { bits: BitVector =>
          exerciseProcessor ! ExerciseDataCmd(bits)
          "OK"
        }
      }
    }

}
