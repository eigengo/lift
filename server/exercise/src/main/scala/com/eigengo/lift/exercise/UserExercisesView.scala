package com.eigengo.lift.exercise

import akka.actor.{ActorLogging, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentView
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.exercise.ExerciseClassifier.ModelMetadata

object UserExercisesView {
  /** The shard name */
  val shardName = "user-exercises-view"
  /** The props to create the actor on a node */
  val props = Props[UserExercisesView]

  /**
   * A single recorded exercise
   * @param name the name
   * @param intensity the intensity, if known
   */
  case class Exercise(name: ExerciseName, intensity: Option[ExerciseIntensity])

  /**
   * Exercise session groups the props with the list of exercises and metadata of the model that
   * classified them
   *
   * @param sessionProps the session props
   * @param exercises the exercises done
   * @param modelMetadata the model used to classify the exercises
   */
  case class ExerciseSession(sessionProps: SessionProps, exercises: List[Exercise], modelMetadata: ModelMetadata) {
    def withExercise(exercise: Exercise): ExerciseSession = copy(exercises = exercises :+ exercise)
  }

  /**
   * The key in the exercises map
   * @param sessionId the session id
   * @param metadata the model metadata
   * @param props the session
   */
  case class SessionKey(sessionId: SessionId, metadata: ModelMetadata, props: SessionProps)

  /**
   * The summary of the exercise session
   * @param id the session id
   * @param sessionProps the session props
   */
  case class SessionSummary(id: SessionId, sessionProps: SessionProps)

  /**
   * All user's exercises
   * @param sessions the list of exercises
   */
  case class Exercises(sessions: Map[SessionId, ExerciseSession]) extends AnyVal {
    def withExercise(sessionId: SessionId, modelMetadata: ModelMetadata, sessionProps: SessionProps, exercise: Exercise): Exercises = {
      sessions.get(sessionId) match {
        case None ⇒ copy(sessions = sessions + (sessionId → ExerciseSession(sessionProps, exercises = List(exercise), modelMetadata = modelMetadata)))
        case Some(exercises) ⇒ copy(sessions = sessions + (sessionId → exercises.withExercise(exercise)))
      }
    }
    
    def get(sessionId: SessionId): Option[ExerciseSession] = sessions.get(sessionId)

    def summary: List[SessionSummary] = sessions.collect {
      case (id, ExerciseSession(p, _, _)) ⇒ SessionSummary(id, p)
    }.toList
  }

  /**
   * Companion object for our state
   */
  object Exercises {
    val empty: Exercises = Exercises(Map.empty)
  }


  /**
   * Exercise event received for the given session with model metadata and exercise
   * @param sessionId the session identity
   * @param metadata the model metadata
   * @param sessionProps the session props
   * @param exercise the result
   */
  case class ExerciseEvt(sessionId: SessionId, metadata: ModelMetadata, sessionProps: SessionProps, exercise: Exercise)

  /**
   * Query to receive all exercises for the given ``userId``
   * @param userId the user identity
   */
  case class UserGetExerciseSessionsSummary(userId: UserId)

  /**
   * Query to retrieve the exercises in the given ``userId`` and ``sessionId``
   * @param userId the user identity
   * @param sessionId the session identity
   */
  case class UserGetExerciseSession(userId: UserId, sessionId: SessionId)

  /**
   * Query to receive all exercises. The relationship between ``GetUserExercises`` and ``GetExerciseSessionsSummary`` is that
   * ``GetUserExercises`` is sent to the shard coordinator, which locates the appropriate (user-specific) shard,
   * and sends it the ``GetExerciseSessionsSummary`` message
   */
  private case object GetExerciseSessionsSummary

  /**
   * Finds all exercises in the given ``sessionId``. 
   * @param sessionId the session identity
   */
  private case class GetExerciseSession(sessionId: SessionId)

  /**
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case UserGetExerciseSessionsSummary(userId)    ⇒ (userId.toString, GetExerciseSessionsSummary)
    case UserGetExerciseSession(userId, sessionId) ⇒ (userId.toString, GetExerciseSession(sessionId))
  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserGetExerciseSessionsSummary(userId) ⇒ s"${userId.hashCode() % 10}"
    case UserGetExerciseSession(userId, _)      ⇒ s"${userId.hashCode() % 10}"
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
    case ExerciseEvt(sessionId, metadata, sessionProps, exercise) ⇒
      exercises = exercises.withExercise(sessionId, metadata, sessionProps, exercise)

    // query for exercises
    case GetExerciseSessionsSummary =>
      sender() ! exercises.summary
    case GetExerciseSession(sessionId) ⇒
      sender() ! exercises.get(sessionId)
  }
}
