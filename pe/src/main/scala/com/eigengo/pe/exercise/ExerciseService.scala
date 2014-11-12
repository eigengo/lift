package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, ActorContext}
import com.eigengo.pe.{timeouts, LiftMarshallers}
import scodec.bits.BitVector
import spray.routing.{HttpServiceActor, HttpService}

import scala.concurrent.ExecutionContext

trait ExerciseService extends HttpService with LiftMarshallers {
  import Exercise._
  def exercise: ActorRef = Exercise.lookup

  val exerciseProcessorRoute =
    path("exercise") {
      post {
        handleWith { bits: BitVector =>
          exercise ! ExerciseDataCmd(UUID.fromString("091284FA-2044-435E-BC6B-0E5EE34A6C77"), bits)
          "OK"
        }
      }
    }

}
