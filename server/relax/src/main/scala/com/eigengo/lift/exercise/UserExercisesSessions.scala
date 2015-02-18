package com.eigengo.lift.exercise

import java.util.{Calendar, Date}

import akka.actor.{ActorRef, ActorLogging, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{SnapshotOffer, PersistentView}
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.notification.NotificationProtocol.DataMessagePayload
import com.eigengo.lift.profile.UserProfileNotifications

object UserExercisesSessions {
  import UserExercises._
  /** The shard name */
  val shardName = "user-exercises-view"
  /** The props to create the actor on a node */
  def props(notification: ActorRef, profile: ActorRef) = Props(classOf[UserExercisesSessions], notification, profile)

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
      if (kie.isEmpty) 0.5 else kie.sum / kie.size
    }

    def withMetric(metric: Metric): ExerciseSet = {
      val newExercises = exercises.map(e ⇒ Exercise(e.name, e.intensity, Some(metric)))
      copy(exercises = newExercises)
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
  case class ExerciseSession(id: SessionId, sessionProps: SessionProperties, sets: List[ExerciseSet]) {
    def withNewExerciseSet(set: ExerciseSet): ExerciseSession = {
      if (set.isEmpty) this else copy(sets = sets :+ set)
    }

    lazy val intensity: ExerciseIntensity = if (sets.isEmpty) 0 else sets.map(_.intensity).sum / sets.size
  }

  /**
   * The summary of the exercise session
   * @param id the session id
   * @param sessionProps the session props
   * @param setIntensities the averaged set intensities
   */
  case class SessionSummary(id: SessionId, sessionProps: SessionProperties, setIntensities: Array[Double])

  /**
   * Session itensity summary. Compares actual and planned intensitites.
   *
   * @param intended planned
   * @param actual actual
   */
  case class SessionIntensity(intended: ExerciseIntensity, actual: ExerciseIntensity)

  /**
   * The summary of exercise session dates
   * @param date the date
   * @param sessionIntensities the average intensities of all sessions done on the day
   */
  case class SessionDate(date: Date, sessionIntensities: List[SessionIntensity])

  /** Ordering on SessionSummary */
  private implicit object SessionSummaryOrdering extends Ordering[SessionSummary] {
    override def compare(x: SessionSummary, y: SessionSummary): Int = y.sessionProps.startDate.compareTo(x.sessionProps.startDate)
  }

  implicit class RichDate(date: Date) {
    def midnight: Date = {
      val calendar = Calendar.getInstance()

      calendar.setTime(date)
      calendar.set(Calendar.HOUR_OF_DAY, 0)
      calendar.set(Calendar.MINUTE, 0)
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)

      calendar.getTime
    }
  }

  /**
   * All user's exercises
   * @param sessions the list of exercises
   */
  case class Sessions(sessions: List[ExerciseSession]) extends AnyVal {
    
    /**
     * Adds a session to the exercises
     * @return the exercises with new session
     */
    def withNewSession(session: ExerciseSession): Sessions = copy(sessions = sessions :+ session)

    /**
     * Removes a session
     * @param id the session to be removed
     * @return the exercises without the specified session
     */
    def withoutSession(id: SessionId): Sessions = copy(sessions = sessions.filterNot(_.id == id))

    /**
     * Gets a session identified by ``sessionId``
     * @param sessionId the session identity
     * @return maybe the session
     */
    def get(sessionId: SessionId): Option[ExerciseSession] = sessions.find(_.id == sessionId)

    /**
     * Computes summary of sessions between the given dates
     * @return all sessions summary
     */
    def summary(startDate: Date, endDate: Date): List[SessionSummary] = sessions.flatMap { s ⇒
      val ssd = s.sessionProps.startDate.midnight
      if (ssd.compareTo(startDate) >= 0 && ssd.compareTo(endDate) <= 0) {
        val intensities = s.sets.map(_.intensity).toArray
        Some(SessionSummary(s.id, s.sessionProps, intensities))
      } else None
    }.sorted

    /**
     * Computes the session dates
     * @return the session dates
     */
    def dates: List[SessionDate] = sessions.groupBy(_.sessionProps.startDate.midnight).map { case (date, exerciseSessions) ⇒
      SessionDate(date, exerciseSessions.map(s ⇒ SessionIntensity(s.sessionProps.intendedIntensity, s.intensity)))
    }.toList
  }

  /**
   * Companion object for our state
   */
  object Sessions {
    val empty: Sessions = Sessions(List.empty)
  }

  /**
   * Query to receive all exercises for the given ``userId`` between the given dates
   * @param userId the user identity
   * @param startDate the start date
   * @param endDate the end date
   */
  case class UserGetExerciseSessionsSummary(userId: UserId, startDate: Date, endDate: Date)

  /**
   * Query to receive all session dates for the given ``userId``
   * @param userId the user identity
   */
  case class UserGetExerciseSessionsDates(userId: UserId)

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
   * @param startDate the start date
   * @param endDate the end date
   */
  private case class GetExerciseSessionsSummary(startDate: Date, endDate: Date)

  /**
   * Get session dates
   */
  private case object GetExerciseSessionsDates

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
    case UserGetExerciseSessionsSummary(userId, s, e) ⇒ (userId.toString, GetExerciseSessionsSummary(s, e))
    case UserGetExerciseSessionsDates(userId)         ⇒ (userId.toString, GetExerciseSessionsDates)
    case UserGetExerciseSession(userId, sessionId)    ⇒ (userId.toString, GetExerciseSession(sessionId))
  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserGetExerciseSessionsSummary(userId, _, _) ⇒ s"${userId.hashCode() % 10}"
    case UserGetExerciseSessionsDates(userId)         ⇒ s"${userId.hashCode() % 10}"
    case UserGetExerciseSession(userId, _)            ⇒ s"${userId.hashCode() % 10}"
  }

}

class UserExercisesSessions(notification: ActorRef, userProfile: ActorRef) extends PersistentView with ActorLogging
  with AutoPassivation with UserProfileNotifications {
  import UserExercisesSessions._
  import UserExercises._
  import scala.concurrent.duration._

  // our internal state
  private var exercises = Sessions.empty

  // values from the profile
  private val userId = UserId(self.path.name)
  private val notificationSender = newNotificationSender(userId, notification, userProfile)

  // we'll hang around for 360 seconds, just like the exercise sessions
  context.setReceiveTimeout(360.seconds)

  override def autoUpdateInterval: FiniteDuration = 1.second
  override def autoUpdate: Boolean = true

  override val viewId: String = s"user-exercises-sessions-${userId.toString}"
  override val persistenceId: String = s"user-exercises-${userId.toString}"

  private lazy val queries: Receive = {
    // query for exercises
    case GetExerciseSessionsSummary(startDate, endDate) ⇒
      log.debug("GetExerciseSessionsSummary: from userspace.")
      sender() ! exercises.summary(startDate, endDate)
    case GetExerciseSessionsDates ⇒
      log.debug("GetExerciseSessionsDates: from userspace.")
      sender() ! exercises.dates
    case GetExerciseSession(sessionId) ⇒
      log.debug("GetExerciseSession: from userspace.")
      sender() ! exercises.get(sessionId)
  }

  private lazy val notExercising: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Sessions) ⇒
      log.info("SnapshotOffer: not exercising -> not exercising.")
      exercises = offeredSnapshot

    case SessionStartedEvt(sessionId, sessionProps) if isPersistent ⇒
      log.info(s"SessionStartedEvt($sessionId, $sessionProps): not exercising -> exercising.")
      context.become(exercising(ExerciseSession(sessionId, sessionProps, List.empty)).orElse(queries))

    case SessionDeletedEvt(sessionId) if isPersistent ⇒
      log.info(s"SessionDeletedEvt($sessionId): not exercising -> not exercising")
      exercises = exercises.withoutSession(sessionId)
      saveSnapshot(exercises)
  }

  private def inASet(session: ExerciseSession, set: ExerciseSet): Receive = {
    case ExerciseEvt(_, metadata, exercise) if isPersistent ⇒
      log.debug("ExerciseEvt: in a set -> in a set.")
      context.become(inASet(session, set.withNewExercise(metadata, exercise)).orElse(queries))
    case NoExerciseEvt(_, metadata) if isPersistent ⇒
      log.debug("NoExerciseEvt: in a set -> exercising.")
      context.become(exercising(session.withNewExerciseSet(set)).orElse(queries))
    case ExerciseSetExplicitMarkEvt(_) ⇒
      log.debug("ExerciseSetExplicitMarkEvt: in a set -> exercising.")
      context.become(exercising(session.withNewExerciseSet(set)).orElse(queries))

    case ExerciseSetExerciseMetricEvt(_, metric) ⇒
      log.debug("ExerciseSetExerciseMetricEvt: in a set -> exercising.")
      context.become(inASet(session, set.withMetric(metric)).orElse(queries))

    case SessionEndedEvt(_) if isPersistent ⇒
      log.info("SessionEndedEvt: in a set -> not exercising.")
      exercises = exercises.withNewSession(session.withNewExerciseSet(set))
      notificationSender ! DataMessagePayload("{}")
      saveSnapshot(exercises)

      context.become(notExercising.orElse(queries))
  }
  
  private def exercising(session: ExerciseSession): Receive = {
    case ExerciseEvt(_, metadata, exercise) if isPersistent ⇒
      log.debug("ExerciseEvt: exercising -> in a set.")
      context.become(inASet(session, ExerciseSet(metadata, exercise)).orElse(queries))

    case ExerciseSetExplicitMarkEvt(_) ⇒
      log.debug("ExerciseSetExplicitMarkEvt: exercising -> in a set.")
      context.become(inASet(session, ExerciseSet.empty).orElse(queries))

    case SessionEndedEvt(_) if isPersistent ⇒
      log.info("SessionEndedEvt: exercising -> not exercising.")
      exercises = exercises.withNewSession(session)
      notificationSender ! DataMessagePayload("{}")
      saveSnapshot(exercises)

      context.become(notExercising.orElse(queries))
  }

  override def receive: Receive = withPassivation {
    notExercising.orElse(queries)
  }
}
