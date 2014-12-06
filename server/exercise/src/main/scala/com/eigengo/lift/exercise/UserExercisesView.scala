package com.eigengo.lift.exercise

import akka.actor.{ActorLogging, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{SnapshotOffer, PersistentView}
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.exercise.ExerciseClassifier.ModelMetadata

object UserExercisesView {
  /** The shard name */
  val shardName = "user-exercises-view"
  /** The props to create the actor on a node */
  val props = Props[UserExercisesView]

  /**
   * A single recorded exercise
   *
   * @param name the name
   * @param intensity the intensity, if known
   */
  case class Exercise(name: ExerciseName, intensity: Option[Double] /* Ideally, this would be Option[ExerciseIntensity], but Json4s is being silly */)

  /**
   * A set contains list of exercises
   * @param exercises the exercises in the set
   */
  case class ExerciseSet(exercises: List[Exercise]) {
    lazy val isEmpty = exercises.isEmpty
    def withExercise(modelMetadata: ModelMetadata, exercise: Exercise): ExerciseSet = copy(exercises :+ exercise)
  }
  
  object ExerciseSet {
    def apply(modelMetadata: ModelMetadata, exercise: Exercise): ExerciseSet = {
      empty.withExercise(modelMetadata, exercise)
    }
    val empty = ExerciseSet(List.empty)
  }

  /**
   * Exercise session groups the props with the list of exercises and metadata of the model that
   * classified them
   *
   * @param id the session id
   * @param sessionProps the session props
   * @param sets the exercise sets done
   */
  case class ExerciseSession(id: SessionId, sessionProps: SessionProps, sets: List[ExerciseSet]) {
    def withExerciseSet(set: ExerciseSet): ExerciseSession = {
      if (set.isEmpty) this else copy(sets = sets :+ set)
    }
  }

  /**
   * The summary of the exercise session
   * @param id the session id
   * @param sessionProps the session props
   */
  case class SessionSummary(id: SessionId, sessionProps: SessionProps)

  /** Ordering on SessionSummary */
  private implicit object SessionSummaryOrdering extends Ordering[SessionSummary] {
    override def compare(x: SessionSummary, y: SessionSummary): Int = y.sessionProps.startDate.compareTo(x.sessionProps.startDate)
  }

  /**
   * All user's exercises
   * @param sessions the list of exercises
   */
  case class Exercises(sessions: List[ExerciseSession]) extends AnyVal {
    
    /**
     * Adds a session to the exercises
     * @return the exercises with new session
     */
    def withNewSession(session: ExerciseSession): Exercises = copy(sessions = sessions :+ session)


    /**
     * Gets a session identified by ``sessionId``
     * @param sessionId the session identity
     * @return maybe the session
     */
    def get(sessionId: SessionId): Option[ExerciseSession] = sessions.find(_.id == sessionId)

    /**
     * Computes summary of all sessions
     * @return all sessions summary
     */
    def summary: List[SessionSummary] = sessions.map(s ⇒ SessionSummary(s.id, s.sessionProps)).sorted
  }

  /**
   * Companion object for our state
   */
  object Exercises {
    val empty: Exercises = Exercises(List.empty)
  }

  /**
   * The session has started
   * @param sessionId the session identity
   * @param sessionProps the session props
   */
  case class SessionStartedEvt(sessionId: SessionId, sessionProps: SessionProps)

  /**
   * The session has ended
   * @param sessionId the session id
   */
  case class SessionEndedEvt(sessionId: SessionId)

  /**
   * Exercise event received for the given session with model metadata and exercise
   * @param sessionId the session identity
   * @param metadata the model metadata
   * @param sessionProps the session props
   * @param exercise the result
   */
  case class ExerciseEvt(sessionId: SessionId, metadata: ModelMetadata, sessionProps: SessionProps, exercise: Exercise)

  /**
   * No exercise: rest or just being lazy
   * @param sessionId the session identity
   * @param metadata the model metadata
   */
  case class NoExerciseEvt(sessionId: SessionId, metadata: ModelMetadata)

  /**
   * Too much rest
   * @param sessionId the session identity
   */
  case class TooMuchRestEvt(sessionId: SessionId)

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

  private lazy val queries: Receive = {
    // query for exercises
    case GetExerciseSessionsSummary ⇒
      sender() ! exercises.summary
    case GetExerciseSession(sessionId) ⇒
      sender() ! exercises.get(sessionId)

    case x ⇒ log.warning("Missed " + x)
  }

  private lazy val notExercising: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Exercises) ⇒
      exercises = offeredSnapshot

    case SessionStartedEvt(sessionId, sessionProps) if isPersistent ⇒
      context.become(exercising(ExerciseSession(sessionId, sessionProps, List.empty)).orElse(queries))
  }

  private def inASet(session: ExerciseSession, set: ExerciseSet): Receive = {
    case ExerciseEvt(_, metadata, _, exercise) if isPersistent ⇒
      log.info("ExerciseEvt: in a set -> in a set (new exercise)")
      context.become(inASet(session, set.withExercise(metadata, exercise)).orElse(queries))
    case NoExerciseEvt(_, metadata) if isPersistent ⇒
      log.info("NoExerciseEvt: in a set -> exercising")
      context.become(exercising(session.withExerciseSet(set)).orElse(queries))
    case TooMuchRestEvt(_) if isPersistent ⇒
      log.info("TooMuchRestEvt: in a set -> exercising")
      context.become(exercising(session.withExerciseSet(set)).orElse(queries))

    case SessionEndedEvt(_) if isPersistent ⇒
      log.info("SessionEndedEvt: in a set -> not exercising")
      exercises = exercises.withNewSession(session.withExerciseSet(set))
      saveSnapshot(exercises)
      context.become(notExercising.orElse(queries))
  }
  
  private def exercising(session: ExerciseSession): Receive = {
    case ExerciseEvt(_, metadata, _, exercise) if isPersistent ⇒
      log.info("ExerciseEvt: exercising -> in a set")
      context.become(inASet(session, ExerciseSet(metadata, exercise)).orElse(queries))

    case TooMuchRestEvt(_) ⇒

    case SessionEndedEvt(_) if isPersistent ⇒
      log.info("SessionEndedEvt: exercising -> not exercising")
      saveSnapshot(exercises)
      exercises = exercises.withNewSession(session)
  }

  override def receive: Receive = withPassivation {
    notExercising.orElse(queries)
  }
}
