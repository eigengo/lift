package com.eigengo.lift.exercise

import akka.actor.Props
import akka.persistence.PersistentView
import com.eigengo.lift.common.UserId
import com.eigengo.lift.exercise.UserExercises.{SessionEndedEvt, SessionStartedEvt, ExerciseEvt}
import com.eigengo.lift.exercise.UserExercisesSessions.ExerciseSession

object UserExercisesStatistics {
  /** The shard name */
  val shardName = "user-exercises-statistics"
  /** The props to create the actor on a node */
  def props() = Props(classOf[UserExercisesStatistics])

}

class UserExercisesStatistics extends PersistentView {
  import scala.concurrent.duration._
  private val userId = UserId(self.path.name)

  // we'll hang around for 360 seconds, just like the exercise sessions
  context.setReceiveTimeout(360.seconds)

  override def autoUpdateInterval: FiniteDuration = 1.second
  override def autoUpdate: Boolean = true

  override val viewId: String = s"user-exercises-statistics-${userId.toString}"
  override val persistenceId: String = s"user-exercises-${userId.toString}"

  lazy val notExercising: Receive = {
    case SessionStartedEvt(sessionId, sessionProperties) if isPersistent ⇒
      context.become(exercising(sessionProperties))
  }

  private def exercising(sessionProperties: SessionProperties): Receive = {
    case ExerciseEvt(_, metadata, exercise) if isPersistent ⇒

    case SessionEndedEvt(_) ⇒ context.become(notExercising)
  }

  override def receive: Receive = notExercising
}
