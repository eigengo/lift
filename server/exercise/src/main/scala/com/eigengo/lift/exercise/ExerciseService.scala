package com.eigengo.lift.exercise

import com.eigengo.lift.exercise.ExerciseDataProcessor.UserExerciseDataCmd
import com.eigengo.lift.exercise.UserExercises.UserGetAllExercises
import scodec.bits.BitVector
import spray.routing.HttpService

trait ExerciseService extends HttpService with LiftMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  implicit val _ = actorRefFactory.dispatcher
  def userExerciseProcessor = ExerciseDataProcessor.lookup
  def userExercises = UserExercises.lookup

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
