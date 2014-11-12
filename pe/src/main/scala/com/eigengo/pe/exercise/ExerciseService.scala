package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorSystem, ActorContext}
import com.eigengo.pe.{timeouts, LiftMarshallers}
import scodec.bits.BitVector
import spray.routing.{HttpServiceActor, HttpService}

import scala.concurrent.ExecutionContext

trait ExerciseService extends HttpService with LiftMarshallers {
  import Exercise._

  val userExerciseProcessorRoute =
    path("exercise") {
      post {
        handleWith { bits: BitVector =>
          Exercise.lookup ! ExerciseDataCmd(UUID.fromString("091284FA-2044-435E-BC6B-0E5EE34A6C77"), bits)
          "OK"
        }
      }
    }

}
