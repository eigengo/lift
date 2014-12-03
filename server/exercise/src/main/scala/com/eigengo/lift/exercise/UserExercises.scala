package com.eigengo.lift.exercise

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentActor
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.exercise.AccelerometerData._
import com.eigengo.lift.exercise.ExerciseClassifier.{Classify, FullyClassifiedExercise, UnclassifiedExercise}
import com.eigengo.lift.exercise.UserExercises._
import com.eigengo.lift.exercise.UserExercisesView.{Exercise, ExerciseEvt}
import com.eigengo.lift.profile.NotificationProtocol
import NotificationProtocol.{MobileDestination, PushMessage, WatchDestination}
import com.typesafe.config.ConfigFactory
import scodec.bits.BitVector
import scala.collection.JavaConversions._
import scala.language.postfixOps
import scalaz.\/

/**
 * User + list of exercises companion
 */
object UserExercises {

  /** The shard name */
  val shardName = "user-exercises"
  /** The props to create the actor on a node */
  def props(profile: ActorRef, exerciseClassifiers: ActorRef) = Props(classOf[UserExercises], profile, exerciseClassifiers)
  /** The props to create the actor within the context of a shard */
  def shardingProps(profile: ActorRef, exerciseClassifiers: ActorRef): Option[Props] = {
    val roles = ConfigFactory.load().getStringList("akka.cluster.roles")
    roles.find("exercise" ==).map(_ => props(profile, exerciseClassifiers))
  }

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param sessionId the session identity
   * @param bits the submitted bits
   */
  case class UserExerciseDataProcess(userId: UserId, sessionId: SessionId, bits: BitVector)

  /**
   * Process exercise data for the given session
   * @param sessionId the session identifier
   * @param bits the exercise data bits
   */
  private case class ExerciseDataProcess(sessionId: SessionId, bits: BitVector)

  /**
   * Starts the user exercise session
   * @param userId the user identity
   * @param session the session details
   */
  case class UserExerciseSessionStart(userId: UserId, session: Session)

  /**
   * Ends the user exercise session
   * @param userId the user identity
   * @param sessionId the generated session identity
   */
  case class UserExerciseSessionEnd(userId: UserId, sessionId: SessionId)

  /**
   * The session has started
   * @param session the session identity
   */
  private case class ExerciseSessionStart(session: Session)

  /**
   * The session has ended
   * @param sessionId the session identity
   */
  private case class ExerciseSessionEnd(sessionId: SessionId)

  /**
   * Accelerometer data for the given session
   * @param sessionId the session identity
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
class UserExercises(profile: ActorRef, exerciseClasssifiers: ActorRef) extends PersistentActor with ActorLogging with AutoPassivation {
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

  private def exercising(session: Session): Receive = withPassivation {
    case ExerciseDataProcess(id, bits) if id == session.id ⇒
      val result = decodeAll(bits, Nil)
      validateData(result).fold(
        { err ⇒ sender() ! \/.left(err) },
        { evt ⇒
          persist(Classify(session, evt))(exerciseClasssifiers !)
          sender() ! \/.right("Data accepted")
        }
      )

    case FullyClassifiedExercise(metadata, `session`, confidence, name, intensity) ⇒
      if (confidence > confidenceThreshold) {
        persist(ExerciseEvt(metadata, session, Exercise(name, intensity))) { evt ⇒
          intensity.foreach { i ⇒
            if (i << session.intendedIntensity) profile ! PushMessage(userId, "Harder!", None, Some("default"), Seq(MobileDestination, WatchDestination))
            if (i >> session.intendedIntensity) profile ! PushMessage(userId, "Easier!", None, Some("default"), Seq(MobileDestination, WatchDestination))
          }
        }
      }

    case UnclassifiedExercise(_, `session`) ⇒
      profile ! PushMessage(userId, "Missed exercise", None, None, Seq(WatchDestination))

    case cmd@ExerciseSessionEnd(id) if session.id == id ⇒
      persist(cmd) { evt ⇒
        context.become(notExercising)
      }
      sender() ! \/.right("Session ended")
  }

  private def notExercising: Receive = withPassivation {
    case FullyClassifiedExercise(metadata, session, confidence, name, intensity) ⇒
      if (confidence > confidenceThreshold) {
        persist(ExerciseEvt(metadata, session, Exercise(name, intensity)))(_ ⇒ ())
      }
    case cmd@ExerciseSessionStart(session) ⇒
      persist(cmd) { evt ⇒
        context.become(exercising(session))
      }
      sender() ! \/.right("Session started")
  }

  // after recovery is complete, we move to processing commands
  override def receiveCommand: Receive = notExercising

}
