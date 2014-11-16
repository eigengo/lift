package com.eigengo.lift.exercise

import com.eigengo.lift.exercise.ExerciseDataProcessor.UserExerciseDataProcess
import com.eigengo.lift.exercise.UserExercises.{UserExerciseSessionEnd, UserExerciseSessionStart, UserGetAllExercises}
import scodec.bits.BitVector
import spray.routing.{Directives, HttpService}

import scala.concurrent.ExecutionContext

trait ExerciseService extends Directives with ExerciseMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def exerciseRoute(boot: ExerciseBoot)(implicit ec: ExecutionContext) =
    path("exercise" / UserIdValue) { userId ⇒
      post {
        handleWith { session: Session ⇒
          (boot.userExercises ? UserExerciseSessionStart(userId, session)).map(_.toString)
        }
      } ~
      get {
        complete {
          (boot.userExercises ? UserGetAllExercises(userId)).map(_.toString)
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue) { (userId, sessionId) ⇒
      put {
        handleWith { bits: BitVector ⇒
          (boot.exerciseDataProcessor ? UserExerciseDataProcess(userId, sessionId, bits)).map(_.toString)
        }
      } ~
      delete {
        complete {
          (boot.userExercises ? UserExerciseSessionEnd(userId, sessionId)).map(_.toString)
        }
      }
    }

}
