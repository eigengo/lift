package com.eigengo.pe.exercise

import java.util.Date

import akka.actor.{ActorLogging, ActorRefFactory, Props, ReceiveTimeout}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.pe.{AccelerometerData, Actors, SessionId, UserId}

import scala.language.postfixOps

/**
 * User + list of exercises companion
 */
object UserExercises {
  import com.eigengo.pe.exercise.ExerciseClassifier._

  /** The shard name */
  val shardName = "user-exercises-shard"
  /** The props to create the actor on a node */
  val props = Props[UserExercises]
  /** Convenience lookup function */
  def lookup(implicit arf: ActorRefFactory) = Actors.shard.lookup(arf, shardName)

  /**
   * The event with processed fitness data into ``List[AccelerometerData]``
   * @param data the accelerometer data
   */
  case class UserExerciseDataEvt(userId: UserId, sessionId: SessionId, data: AccelerometerData)

  /**
   * A single recorded exercise
   * @param sessionId the exercise session
   * @param date the exercise date (when it was classified)
   * @param name the name
   * @param intensity the intensity, if known
   */
  case class Exercise(sessionId: SessionId, date: Date, name: ExerciseName, intensity: Option[ExerciseIntensity])

  /**
   * All user's exercises
   * @param exercises the list of exercises
   */
  case class Exercises(exercises: List[Exercise]) {
    val confidenceThreshold = 0.5
    def +(exercise: ClassifiedExercise) = {
      if (exercise.confidence > confidenceThreshold) {
        this
      } else this
    }
  }

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
    case UserExerciseDataEvt(userId, data) ⇒ (userId.toString, data)
    case UserGetAllExercises(userId) ⇒ (userId.toString, GetExercises)
  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserGetAllExercises(userId) ⇒ "global"
    case UserExerciseDataEvt(userId, _) ⇒ "global"
  }

}

/**
 * Models each user's exercises as its state, which is updated upon receiving and classifying the
 * ``AccelerometerData``. It also provides the query for the current state.
 */
class UserExercises extends PersistentActor with ActorLogging {
  import akka.contrib.pattern.ShardRegion.Passivate
  import com.eigengo.pe.exercise.ExerciseClassifier._
  import com.eigengo.pe.exercise.UserExercises._

import scala.concurrent.duration._

  // the shard lives for the specified timeout seconds before passivating
  context.setReceiveTimeout(120.seconds)

  // our unique persistenceId; the self.path.name is provided by ``UserExercises.idExtractor``,
  // hence, self.path.name is the String representation of the userId UUID.
  override val persistenceId: String = s"user-exercises-${self.path.name}"
  // our internal state
  private var exercises = List.empty[ClassifiedExercise]

  // when this actor recovers (i.e. moving from "not present" to "present"), it is sent messages that
  // we handle to get to the state that the actor was before it was removed.
  override def receiveRecover: Receive = {
    // restore from snapshot
    case SnapshotOffer(_, offeredSnapshot: List[ClassifiedExercise @unchecked]) ⇒
      log.debug(s"SnapshotOffer in AS ${self.path.toString}")
      exercises = offeredSnapshot

    // reclassify the exercise in AccelerometerData
    case ad@AccelerometerData(_, _) ⇒
      ExerciseClassifiers.lookup ! ad
  }

  // after recovery is complete, we move to processing commands
  override def receiveCommand: Receive = {
    // passivation support
    case ReceiveTimeout ⇒
      context.parent ! Passivate(stopMessage = 'stop)
    case 'stop ⇒
      context.stop(self)

    // classify the exercise in AccelerometerData
    case evt@AccelerometerData(_, _) ⇒
      log.debug(s"AccelerometerData in AS ${self.path.toString}")
      persist(evt)(ExerciseClassifiers.lookup !)

    // classification results received
    case e@ClassifiedExercise(confidence, exercise) ⇒
      log.debug(s"ClassificationResult in AS ${self.path.toString}")
      if (confidence > 0.0) {
        exercises = e :: exercises
        // TODO: Move me
        // exercise.foreach(e ⇒ PushNotification.lookup ! DefaultMessage(userId, e, Some(1), Some("default")))
      }
      saveSnapshot(exercises)
      log.debug(s"Now with ${exercises.size} exercises")

    // query for exercises
    case GetExercises =>
      log.debug(s"GetExercises in AS ${self.path.toString}")
      sender() ! exercises
  }

}
