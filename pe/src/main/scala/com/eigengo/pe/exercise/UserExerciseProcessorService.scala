package com.eigengo.pe.exercise

import java.util.UUID

import com.eigengo.pe.LiftMarshallers
import scodec.bits.BitVector
import spray.routing.HttpService

trait UserExerciseProcessorService extends HttpService with LiftMarshallers {
  import com.eigengo.pe.exercise.Exercise._
  def exerciseProcessor = actorRefFactory.actorSelection("/user/exercise")

  val userExerciseProcessorRoute =
    path("exercise") {
      post {
        handleWith { bits: BitVector =>
          exerciseProcessor ! ExerciseDataCmd(UUID.fromString("091284FA-2044-435E-BC6B-0E5EE34A6C77"), bits)
          "OK"
        }
      }
    }

}
