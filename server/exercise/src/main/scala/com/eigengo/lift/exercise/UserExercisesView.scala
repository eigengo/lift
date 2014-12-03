package com.eigengo.lift.exercise

import akka.actor.{ActorLogging, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentView
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.exercise.ExerciseClassifier.ModelMetadata
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

object UserExercisesView {
  /** The shard name */
  val shardName = "user-exercises-view"
  /** The props to create the actor on a node */
  val props = Props[UserExercisesView]
  /** The props to create the actor within the context of a shard */
  def shardingProps(): Option[Props] = {
    val roles = ConfigFactory.load().getStringList("akka.cluster.roles")
    roles.find("exercise" ==).map(_ => props)
  }

  /**
   * A single recorded exercise
   * @param name the name
   * @param intensity the intensity, if known
   */
  case class Exercise(name: ExerciseName, intensity: Option[ExerciseIntensity])

  /**
   * The key in the exercises map
   * @param metadata the model metadata
   * @param session the session
   */
  case class SessionKey(metadata: ModelMetadata, session: Session)

  /**
   * All user's exercises
   * @param sessions the list of exercises
   */
  case class Exercises(sessions: Map[SessionKey, List[Exercise]]) extends AnyVal {
    def withExercise(metadata: ModelMetadata, session: Session, exercise: Exercise): Exercises = {
      val key = SessionKey(metadata, session)
      sessions.get(key) match {
        case None ⇒ copy(sessions = sessions + (key → List(exercise)))
        case Some(exercises) ⇒ copy(sessions = sessions + (key → exercises.+:(exercise)))
      }
    }
  }

  /**
   * Companion object for our state
   */
  object Exercises {
    val empty: Exercises = Exercises(Map.empty)
  }


  /**
   * Exercise event received for the given session with model metadata and exercise
   * @param metadata the model metadata
   * @param session the session
   * @param exercise the result
   */
  case class ExerciseEvt(metadata: ModelMetadata, session: Session, exercise: Exercise)

  /**
   * Query to receive all exercises for the given ``userId``
   * @param userId the user identity
   */
  case class UserGetAllExercises(userId: UserId)

  /**
   * Query to receive all exercises. The relationship between ``GetUserExercises`` and ``GetExercises`` is that
   * ``GetUserExercises`` is sent to the shard coordinator, which locates the appropriate (user-specific) shard,
   * and sends it the ``GetExercises`` message
   */
  private case object GetExercises

  /**
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case UserGetAllExercises(userId) ⇒ (userId.toString, GetExercises)
  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserGetAllExercises(userId) ⇒ s"${userId.hashCode() % 10}"
  }

}

class UserExercisesView extends PersistentView with ActorLogging with AutoPassivation {
  import com.eigengo.lift.exercise.UserExercisesView._
  import scala.concurrent.duration._

  // our internal state
  private var exercises = Exercises.empty

  override val passivationTimeout: Duration = 360.seconds
  override val viewId: String = s"user-exercises-view-${self.path.name}"
  override val persistenceId: String = s"user-exercises-${self.path.name}"

  override def receive: Receive = withPassivation {
    // exercise received
    case ExerciseEvt(metadata, session, exercise) ⇒
      exercises = exercises.withExercise(metadata, session, exercise)

    // query for exercises
    case GetExercises =>
      sender() ! exercises
  }
}
