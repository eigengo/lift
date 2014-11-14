package com.eigengo.pe.exercise

import akka.actor.ActorRef
import com.eigengo.pe.LiftMarshallers
import scodec.bits.BitVector
import spray.routing.HttpService

trait ExerciseService extends HttpService with LiftMarshallers {
  import akka.pattern.ask
  import com.eigengo.pe.Timeouts.defaults._
  import com.eigengo.pe.exercise.UserExerciseDataProcessor._
  import com.eigengo.pe.exercise.UserExercises._

  implicit val _ = actorRefFactory.dispatcher
  def userExerciseProcessor: ActorRef = UserExerciseDataProcessor.lookup
  def userExercises: ActorRef = UserExercises.lookup

  val exerciseRoute =
    path("exercise" / UserIdValue / SessionIdValue) { (userId, sessionId) ⇒
      post {
        handleWith { bits: BitVector ⇒
          (userExerciseProcessor ? UserExerciseDataCmd(userId, sessionId, bits)).map(_.toString)
        }
      } ~
      get {
        complete {
          (userExercises ? UserGetAllExercises(userId)).map(_.toString)
        }
      }
    }

}
