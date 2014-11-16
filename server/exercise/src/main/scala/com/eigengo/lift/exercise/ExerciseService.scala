package com.eigengo.lift.exercise

import akka.actor.ActorRef
import com.eigengo.lift.exercise.ExerciseDataProcessor.UserExerciseDataProcess
import com.eigengo.lift.exercise.UserExercises.{UserExerciseSessionEnd, UserExerciseSessionStart, UserGetAllExercises}
import scodec.bits.BitVector
import spray.routing.Directives

import scala.concurrent.ExecutionContext

trait ExerciseService extends Directives with ExerciseMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def exerciseRoute(userExercises: ActorRef, exerciseDataProcessor: ActorRef)(implicit ec: ExecutionContext) =
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
          (exerciseDataProcessor ? UserExerciseDataProcess(userId, sessionId, bits)).map(_.toString)
        }
      } ~
      delete {
        complete {
          (userExercises ? UserExerciseSessionEnd(userId, sessionId)).map(_.toString)
        }
      }
    }

}
