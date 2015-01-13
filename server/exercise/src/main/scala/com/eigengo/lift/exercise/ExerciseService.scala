package com.eigengo.lift.exercise

import java.util.{Date, UUID}

import akka.actor.ActorRef
import com.eigengo.lift.exercise.UserExercisesProcessor._
import com.eigengo.lift.exercise.UserExercisesSessions._
import com.eigengo.lift.exercise.packet.MultiPacket
import scodec.bits.BitVector
import spray.routing.Directives

import scala.concurrent.ExecutionContext

trait ExerciseService extends Directives with ExerciseMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def exerciseRoute(userExercises: ActorRef, userExercisesView: ActorRef)(implicit ec: ExecutionContext) =
    path("exercise" / "musclegroups") {
      get {
        complete {
          UserExerciseClassifier.supportedMuscleGroups
        }
      }
    } ~
    path("exercise" / UserIdValue) { userId ⇒
      get {
        parameters('startDate.as[Date], 'endDate.as[Date]) { (startDate, endDate) ⇒
          complete {
            (userExercisesView ? UserGetExerciseSessionsSummary(userId, startDate, endDate)).mapTo[List[SessionSummary]]
          }
        } ~
        parameter('date.as[Date]) { date ⇒
          complete {
            (userExercisesView ? UserGetExerciseSessionsSummary(userId, date, date)).mapTo[List[SessionSummary]]
          }
        } ~
        complete {
          (userExercisesView ? UserGetExerciseSessionsDates(userId)).mapTo[List[SessionDate]]
        }
      }
    } ~
    path("exercise" / UserIdValue / "start") { userId ⇒
      post {
        handleWith { sessionProps: SessionProps ⇒
          (userExercises ? UserExerciseSessionStart(userId, sessionProps)).mapRight[UUID]
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue / "end") { (userId, sessionId) ⇒
      post {
        complete {
          (userExercises ? UserExerciseSessionEnd(userId, sessionId)).mapRight[Unit]
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue) { (userId, sessionId) ⇒
      get {
        complete {
          (userExercisesView ? UserGetExerciseSession(userId, sessionId)).mapTo[Option[ExerciseSession]]
        }
      } ~
      put {
        // TODO: content type negotiation
        handleWith { packet: MultiPacket ⇒
          (userExercises ? UserExerciseDataProcessMultiPacket(userId, sessionId, packet)).mapRight[Unit]
        }
      } ~
      delete {
        complete {
          (userExercises ? UserExerciseSessionDelete(userId, sessionId)).mapRight[Unit]
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue / "classification") { (userId, sessionId) ⇒
      post {
        handleWith { exercise: Exercise ⇒
          (userExercises ? UserExerciseExplicitClassificationStart(userId, sessionId, exercise)).mapRight[Unit]
        }
      } ~
      delete {
        complete {
          userExercises ! UserExerciseExplicitClassificationEnd(userId, sessionId)
          ""
        }
      }
    }
}
