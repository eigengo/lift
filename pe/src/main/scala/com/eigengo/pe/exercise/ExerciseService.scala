package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorSystem, ActorContext}
import com.eigengo.pe.{timeouts, LiftMarshallers}
import scodec.bits.BitVector
import spray.routing.HttpService

import scala.concurrent.ExecutionContext

trait ExerciseService extends HttpService with LiftMarshallers {
  import UserExercise._
  import UserExerciseView._
  implicit def system: ActorSystem
  private implicit def dispatcher: ExecutionContext = system.dispatcher
  import akka.pattern.ask
  import timeouts.defaults._

  val userExerciseProcessorRoute =
    path("exercise") {
      post {
        handleWith { bits: BitVector =>
          UserExercise.lookup ! ExerciseDataCmd(UUID.fromString("091284FA-2044-435E-BC6B-0E5EE34A6C77"), bits)
          "OK"
        }
      } ~
      get {
        complete {
          // TODO: proper marshalling
          (UserExerciseView.lookup ? GetExercises(UUID.fromString("091284FA-2044-435E-BC6B-0E5EE34A6C77"))).map(_.toString)
        }
      }
    }

}
