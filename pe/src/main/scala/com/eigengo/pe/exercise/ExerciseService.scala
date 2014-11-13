package com.eigengo.pe.exercise

import akka.actor.{ActorSelection, ActorRef}
import com.eigengo.pe.LiftMarshallers
import scodec.bits.BitVector
import spray.routing.HttpService

trait ExerciseService extends HttpService with LiftMarshallers {
  import ExerciseProcessor._
  import UserExercises._
  import akka.pattern.ask
  import com.eigengo.pe.timeouts.defaults._

  implicit val _ = actorRefFactory.dispatcher
  def exercise: ActorSelection = ExerciseProcessor.lookup
  def userExercises: ActorRef = UserExercises.lookup

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
          (userExercises ? GetUserExercises(userId)).map(_.toString)
        }
      }
    }

}
