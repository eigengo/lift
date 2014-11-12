package com.eigengo.pe.exercise

import akka.actor.{ActorSelection, ActorRef}
import com.eigengo.pe.LiftMarshallers
import scodec.bits.BitVector
import spray.routing.HttpService

trait ExerciseService extends HttpService with LiftMarshallers {
  import ExerciseProcessor._
  import UserExercise._
  import akka.pattern.ask
  import com.eigengo.pe.timeouts.defaults._

  implicit val _ = actorRefFactory.dispatcher
  def exercise: ActorSelection = ExerciseProcessor.lookup
  def userExercise: ActorRef = UserExercise.lookup

  val exerciseRoute =
    path("exercise" / JavaUUID) { userId ⇒
      post {
        handleWith { bits: BitVector =>
          exercise ! ExerciseDataCmd(userId, bits)
          "OK"
        }
      } ~
      get {
        complete {
          (userExercise ? GetUserExercises(userId)).map(_.toString)
        }
      }
    }

}
