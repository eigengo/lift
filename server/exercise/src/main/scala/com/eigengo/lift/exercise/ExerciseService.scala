package com.eigengo.lift.exercise

import java.util.UUID

import akka.actor.ActorRef
import com.eigengo.lift.exercise.ExerciseClassifiers.{MuscleGroup, GetMuscleGroups}
import com.eigengo.lift.exercise.UserExercises.{UserExerciseDataProcess, UserExerciseSessionEnd, UserExerciseSessionStart}
import com.eigengo.lift.exercise.UserExercisesView.{Exercises, UserGetAllExercises}
import scodec.bits.BitVector
import spray.routing.Directives

import scala.concurrent.ExecutionContext

trait ExerciseService extends Directives with ExerciseMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def exerciseRoute(userExercises: ActorRef, userExercisesView: ActorRef, exerciseClassifiers: ActorRef)(implicit ec: ExecutionContext) =
    path("exercise" / "musclegroups") {
      get {
        complete {
          (exerciseClassifiers ? GetMuscleGroups).mapTo[List[MuscleGroup]]
        }
      }
    } ~
    path("exercise" / UserIdValue) { userId ⇒
      post {
        handleWith { sessionProps: SessionProps ⇒
          (userExercises ? UserExerciseSessionStart(userId, sessionProps)).mapRight[UUID]
        }
      } ~
      get {
        complete {
          (userExercisesView ? UserGetAllExercises(userId)).mapTo[List[SessionProps]]
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue) { (userId, sessionId) ⇒
      put {
        handleWith { bits: BitVector ⇒
          (userExercises ? UserExerciseDataProcess(userId, sessionId, bits)).map(_.toString)
        }
      } ~
      delete {
        complete {
          (userExercises ? UserExerciseSessionEnd(userId, sessionId)).map(_.toString)
        }
      }
    }

}
