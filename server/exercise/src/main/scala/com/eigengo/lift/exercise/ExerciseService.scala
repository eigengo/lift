package com.eigengo.lift.exercise

import com.eigengo.lift.exercise.ExerciseDataProcessor.UserExerciseDataProcess
import com.eigengo.lift.exercise.UserExercises.{UserExerciseSessionEnd, UserExerciseSessionStart, UserGetAllExercises}
import scodec.bits.BitVector
import spray.routing.HttpService

trait ExerciseService extends HttpService with ExerciseMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  implicit val _ = actorRefFactory.dispatcher
  def userExerciseProcessor = ExerciseDataProcessor.lookup
  def userExercises = UserExercises.lookup

  val exerciseRoute =
    path("exercise" / UserIdValue) { userId ⇒
      post {
        handleWith { session: Session ⇒
          (userExercises ? UserExerciseSessionStart(userId, session)).map(_.toString)
        }
      } ~
      get {
        complete {
          (userExercises ? UserGetAllExercises(userId)).map(_.toString)
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue) { (userId, sessionId) ⇒
      put {
        handleWith { bits: BitVector ⇒
          (userExerciseProcessor ? UserExerciseDataProcess(userId, sessionId, bits)).map(_.toString)
        }
      } ~
      delete {
        complete {
          (userExercises ? UserExerciseSessionEnd(userId, sessionId)).map(_.toString)
        }
      }
    }

}
