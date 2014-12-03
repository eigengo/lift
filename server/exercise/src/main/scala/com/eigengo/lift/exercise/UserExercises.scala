package com.eigengo.lift.exercise

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.{SnapshotSelectionCriteria, PersistentActor, Recover}
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.exercise.AccelerometerData._
import com.eigengo.lift.exercise.ExerciseClassifier.{Classify, FullyClassifiedExercise, UnclassifiedExercise}
import com.eigengo.lift.exercise.UserExercises._
import com.eigengo.lift.exercise.UserExercisesView.{Exercise, ExerciseEvt}
import com.eigengo.lift.notification.NotificationProtocol.{MobileDestination, PushMessage, WatchDestination}
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

/**
 * User + list of exercises companion
 */
object UserExercises {

  /** The shard name */
  val shardName = "user-exercises"
  /** The props to create the actor on a node */
  def props(notification: ActorRef, exerciseClassifiers: ActorRef) = Props(classOf[UserExercises], notification, exerciseClassifiers)

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param sessionId the props identity
   * @param bits the submitted bits
   */
  case class UserExerciseDataProcess(userId: UserId, sessionId: SessionId, bits: BitVector)

  /**
   * Process exercise data for the given props
   * @param sessionId the props identifier
   * @param bits the exercise data bits
   */
  private case class ExerciseDataProcess(sessionId: SessionId, bits: BitVector)

  /**
   * Starts the user exercise props
   * @param userId the user identity
   * @param sessionProps the props details
   */
  case class UserExerciseSessionStart(userId: UserId, sessionProps: SessionProps)

  /**
   * Ends the user exercise props
   * @param userId the user identity
   * @param sessionId the generated props identity
   */
  case class UserExerciseSessionEnd(userId: UserId, sessionId: SessionId)

  /**
   * The props has started
   * @param props the props identity
   */
  private case class ExerciseSessionStart(props: SessionProps)

  /**
   * The props has ended
   * @param sessionId the props identity
   */
  private case class ExerciseSessionEnd(sessionId: SessionId)

  /**
   * Accelerometer data for the given props
   * @param sessionId the props identity
   * @param data the data
   */
  private case class ExerciseSessionData(sessionId: SessionId, data: AccelerometerData)

  /**
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case UserExerciseSessionStart(userId, session)        ⇒ (userId.toString, ExerciseSessionStart(session))
    case UserExerciseSessionEnd(userId, sessionId)        ⇒ (userId.toString, ExerciseSessionEnd(sessionId))
    case UserExerciseDataProcess(userId, sessionId, data) ⇒ (userId.toString, ExerciseDataProcess(sessionId, data))
  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserExerciseSessionStart(userId, _)   ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionEnd(userId, _)     ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseDataProcess(userId, _, _) ⇒ s"${userId.hashCode() % 10}"
  }

}

/**
 * Models each user's exercises as its state, which is updated upon receiving and classifying the
 * ``AccelerometerData``. It also provides the query for the current state.
 */
class UserExercises(notification: ActorRef, exerciseClasssifiers: ActorRef) extends PersistentActor with ActorLogging with AutoPassivation {
  import scala.concurrent.duration._

  private val userId = UserId(self.path.name)
  // minimum confidence
  private val confidenceThreshold = 0.5
  // how long until we stop processing
  override val passivationTimeout: Duration = 360.seconds
  // our unique persistenceId; the self.path.name is provided by ``UserExercises.idExtractor``,
  // hence, self.path.name is the String representation of the userId UUID.
  override val persistenceId: String = s"user-exercises-${self.path.name}"

  // when we're recovering, handle the classify events
  override def receiveRecover: Receive = Actor.emptyBehavior

  private def validateData(result: (BitVector, List[AccelerometerData])): \/[String, AccelerometerData] = result match {
    case (BitVector.empty, Nil)    ⇒ \/.left("Empty")
    case (BitVector.empty, h :: t) ⇒
      if (t.forall(_.samplingRate == h.samplingRate)) {
        \/.right(t.foldLeft(h)((res, ad) ⇒ ad.copy(values = ad.values ++ res.values)))
      } else {
        \/.left("Unmatched sampling rates")
      }
    case (_, _)                    ⇒ \/.left("Undecoded input")
  }

  private def exercising(id: SessionId, props: SessionProps): Receive = withPassivation {
    case cmd@ExerciseSessionStart(session) ⇒
      persist(cmd) { evt ⇒
        val id = SessionId.randomId()
        sender() ! \/.right(id)
        context.become(exercising(id, session))
      }

    case ExerciseDataProcess(`id`, bits) ⇒
      val result = decodeAll(bits, Nil)
      validateData(result).fold(
        { err ⇒ sender() ! \/.left(err) },
        { evt ⇒
          persist(Classify(props, evt))(exerciseClasssifiers !)
          sender() ! \/.right(())
        }
      )

    case FullyClassifiedExercise(metadata, `props`, confidence, name, intensity) ⇒
      if (confidence > confidenceThreshold) {
        persist(ExerciseEvt(metadata, props, Exercise(name, intensity))) { evt ⇒
          intensity.foreach { i ⇒
            if (i << props.intendedIntensity) notification ! PushMessage(userId, "Harder!", None, Some("default"), Seq(MobileDestination, WatchDestination))
            if (i >> props.intendedIntensity) notification ! PushMessage(userId, "Easier!", None, Some("default"), Seq(MobileDestination, WatchDestination))
          }
        }
      }

    case UnclassifiedExercise(_, `props`) ⇒
      notification ! PushMessage(userId, "Missed exercise", None, None, Seq(WatchDestination))

    case cmd@ExerciseSessionEnd(`id`) ⇒
      persist(cmd) { evt ⇒
        context.become(notExercising)
      }
      sender() ! \/.right(())
  }

  private def notExercising: Receive = withPassivation {
    case FullyClassifiedExercise(metadata, session, confidence, name, intensity) ⇒
      if (confidence > confidenceThreshold) {
        persist(ExerciseEvt(metadata, session, Exercise(name, intensity)))(_ ⇒ ())
      }
    case cmd@ExerciseSessionStart(session) ⇒
      persist(cmd) { evt ⇒
        val id = SessionId.randomId()
        sender() ! \/.right(id)
        context.become(exercising(id, session))
      }
  }

  // after recovery is complete, we move to processing commands
  override def receiveCommand: Receive = notExercising

}
