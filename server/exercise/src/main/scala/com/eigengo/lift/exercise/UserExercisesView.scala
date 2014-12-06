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
    /** true if empty */
    def isEmpty = exercises.isEmpty

    /** Average set intensity */
    def intensity: ExerciseIntensity = {
      val kie = exercises.filter(_.intensity.isDefined).flatMap(_.intensity)
      kie.sum / kie.size
    }

    def withNewExercise(modelMetadata: ModelMetadata, exercise: Exercise): ExerciseSet = copy(exercises :+ exercise)
  }
  
  object ExerciseSet {
    def apply(modelMetadata: ModelMetadata, exercise: Exercise): ExerciseSet = {
      empty.withNewExercise(modelMetadata, exercise)
    }
    /** Average set intensity */
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
    def withNewExerciseSet(set: ExerciseSet): ExerciseSession = {
      if (set.isEmpty) this else copy(sets = sets :+ set)
    }
  }

  /**
   * The summary of the exercise session
   * @param id the session id
   * @param sessionProps the session props
   * @param setIntensities the averaged set intensities
   */
  case class SessionSummary(id: SessionId, sessionProps: SessionProps, setIntensities: Array[Double])

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
    def summary: List[SessionSummary] = sessions.map { s ⇒
      // http://192.168.0.8:12551/exercise/D3D21230-0323-4D22-9848-CD4C67F4865F/71DB39BE-CFB0-4561-B0E5-5DB936128618
      val intensities = s.sets.map(_.intensity).toArray
      SessionSummary(s.id, s.sessionProps, intensities)
    }.sorted
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
   * @param exercise the result
   */
  case class ExerciseEvt(sessionId: SessionId, metadata: ModelMetadata, exercise: Exercise)

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
  }

  private lazy val notExercising: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Exercises) ⇒
      exercises = offeredSnapshot

    case SessionStartedEvt(sessionId, sessionProps) if isPersistent ⇒
      log.info("SessionStartedEvt: not exercising -> exercising")
      context.become(exercising(ExerciseSession(sessionId, sessionProps, List.empty)).orElse(queries))
  }

  private def inASet(session: ExerciseSession, set: ExerciseSet): Receive = {
    case ExerciseEvt(_, metadata, exercise) if isPersistent ⇒
      log.info("ExerciseEvt: in a set -> in a set")
      context.become(inASet(session, set.withNewExercise(metadata, exercise)).orElse(queries))
    case NoExerciseEvt(_, metadata) if isPersistent ⇒
      log.info("NoExerciseEvt: in a set -> exercising")
      context.become(exercising(session.withNewExerciseSet(set)).orElse(queries))
    case TooMuchRestEvt(_) if isPersistent ⇒
      log.info("TooMuchRestEvt: in a set -> exercising")
      context.become(exercising(session.withNewExerciseSet(set)).orElse(queries))

    case SessionEndedEvt(_) if isPersistent ⇒
      log.info("SessionEndedEvt: in a set -> not exercising")
      exercises = exercises.withNewSession(session.withNewExerciseSet(set))
      saveSnapshot(exercises)
      context.become(notExercising.orElse(queries))
  }
  
  private def exercising(session: ExerciseSession): Receive = {
    case ExerciseEvt(_, metadata, exercise) if isPersistent ⇒
      log.info("ExerciseEvt: exercising -> in a set")
      context.become(inASet(session, ExerciseSet(metadata, exercise)).orElse(queries))

    case TooMuchRestEvt(_) ⇒
      log.info("TooMuchRest: exercising -> exercising")

    case SessionEndedEvt(_) if isPersistent ⇒
      log.info("SessionEndedEvt: exercising -> not exercising")
      saveSnapshot(exercises)
      exercises = exercises.withNewSession(session)
  }

  override def receive: Receive = withPassivation {
    notExercising.orElse(queries)
  }
}
