package com.eigengo.lift.exercise

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.{AutoPassivation, UserId}
import com.eigengo.lift.exercise.ExerciseClassifier._
import com.eigengo.lift.exercise.UserExercises._
import com.eigengo.lift.exercise.UserExercisesTracing._
import com.eigengo.lift.exercise.UserExercisesView._
import com.eigengo.lift.exercise.packet.MultiPacket
import com.eigengo.lift.notification.NotificationProtocol._
import com.eigengo.lift.profile.UserProfileNotifications
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

/**
 * User + list of exercises companion
 */
object UserExercises {

  /** The shard name */
  val shardName = "user-exercises"
  /** The sessionProps to create the actor on a node */
  def props(notification: ActorRef, userProfile: ActorRef, exerciseClassifiers: ActorRef) = Props(classOf[UserExercises], notification, userProfile, exerciseClassifiers)

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param sessionId the sessionProps identity
   * @param bits the submitted bits
   */
  case class UserExerciseDataProcessSinglePacket(userId: UserId, sessionId: SessionId, bits: BitVector)

  /**
   * Remove a session identified by ``sessionId`` for user identified by ``userId``
   * @param userId the user identity
   * @param sessionId the session identity
   */
  case class UserExerciseSessionDelete(userId: UserId, sessionId: SessionId)

  /**
   * User classified exercise start.
   * @param userId user
   * @param sessionId session
   * @param exercise the exercise
   */
  case class UserExerciseExplicitClassificationStart(userId: UserId, sessionId: SessionId, exercise: Exercise)

  /**
   * User classification end
   * @param userId the user
   * @param sesionId the session
   */
  case class UserExerciseExplicitClassificationEnd(userId: UserId, sesionId: SessionId)

  /**
   * Receive multiple packets of data for the given ``userId`` and ``sessionId``. The ``packets`` is a ZIP archive
   * containing multiple files, each representing a single packet. Imagine that this message results in at least
   * one ``UserExerciseDataProcessSinglePacket`` messages.
   *
   * The main notion is that all packets in the archive have been measured *at the same time*. It is possible that
   * the archive contains the following files.
   *
   * {{{
   * ad-watch.dat  (accelerometer data from the watch)
   * ad-mobile.dat (accelerometer data from the mobile)
   * ad-shoe.dat   (accelerometer data from a shoe sensor)
   * hr-watch.dat  (heart rate from the watch)
   * hr-strap.dat  (heart rate from a HR strap)
   * ...
   * }}}
   *
   * The files should be processed accordingly (by examining their content, not their file names), and the exercise
   * classifiers should use all available information to determine the exercise
   *
   * @param userId the user identity
   * @param sessionId the session identity
   * @param packet the archive containing at least one packet
   */
  case class UserExerciseDataProcessMultiPacket(userId: UserId, sessionId: SessionId, packet: MultiPacket)

  /**
   * Process exercise data for the given session
   * @param sessionId the sessionProps identifier
   * @param bits the exercise data bits
   */
  private case class ExerciseDataProcessSinglePacket(sessionId: SessionId, bits: BitVector)

  /**
   * Process exercise data for the given session
   * @param sessionId the sessionProps identifier
   * @param packet the bytes representing an archive with multiple exercise data bits
   */
  private case class ExerciseDataProcessMultiPacket(sessionId: SessionId, packet: MultiPacket)

  /**
   * User classified exercise.
   * @param sessionId session
   * @param exercise the exercise
   */
  private case class ExerciseExplicitClassificationStart(sessionId: SessionId, exercise: Exercise)

  /**
   * User classified exercise.
   * @param sessionId session
   */
  private case class ExerciseExplicitClassificationEnd(sessionId: SessionId)

  /**
   * Starts the user exercise sessionProps
   * @param userId the user identity
   * @param sessionProps the sessionProps details
   */
  case class UserExerciseSessionStart(userId: UserId, sessionProps: SessionProps)

  /**
   * Ends the user exercise sessionProps
   * @param userId the user identity
   * @param sessionId the generated sessionProps identity
   */
  case class UserExerciseSessionEnd(userId: UserId, sessionId: SessionId)

  /**
   * The sessionProps has started
   * @param sessionProps the sessionProps identity
   */
  private case class ExerciseSessionStart(sessionProps: SessionProps)

  /**
   * The sessionProps has ended
   * @param sessionId the sessionProps identity
   */
  private case class ExerciseSessionEnd(sessionId: SessionId)

  /**
   * Remove the specified ``sessionId``
   * @param sessionId the session identity
   */
  private case class ExerciseSessionDelete(sessionId: SessionId)

  /**
   * Accelerometer data for the given sessionProps
   * @param sessionId the sessionProps identity
   * @param data the data
   */
  private case class ExerciseSessionData(sessionId: SessionId, data: AccelerometerData)

  /**
   * Too much rest message that is sent to us if the user is being lazy
   */
  private case object TooMuchRest

  /**
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case UserExerciseSessionStart(userId, session)                            ⇒ (userId.toString, ExerciseSessionStart(session))
    case UserExerciseSessionEnd(userId, sessionId)                            ⇒ (userId.toString, ExerciseSessionEnd(sessionId))
    case UserExerciseDataProcessSinglePacket(userId, sessionId, data)         ⇒ (userId.toString, ExerciseDataProcessSinglePacket(sessionId, data))
    case UserExerciseDataProcessMultiPacket(userId, sessionId, packets)       ⇒ (userId.toString, ExerciseDataProcessMultiPacket(sessionId, packets))
    case UserExerciseSessionDelete(userId, sessionId)                         ⇒ (userId.toString, ExerciseSessionDelete(sessionId))
    case UserExerciseExplicitClassificationStart(userId, sessionId, exercise) ⇒ (userId.toString, ExerciseExplicitClassificationStart(sessionId, exercise))
    case UserExerciseExplicitClassificationEnd(userId, sessionId)             ⇒ (userId.toString, ExerciseExplicitClassificationEnd(sessionId))
  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserExerciseSessionStart(userId, _)                   ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionEnd(userId, _)                     ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseDataProcessSinglePacket(userId, _, _)     ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseDataProcessMultiPacket(userId, _, _)      ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionDelete(userId, _)                  ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseExplicitClassificationStart(userId, _, _) ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseExplicitClassificationEnd(userId, _)      ⇒ s"${userId.hashCode() % 10}"
  }

}

/**
 * Models each user's exercises as its state, which is updated upon receiving and classifying the
 * ``AccelerometerData``. It also provides the query for the current state.
 */
class UserExercises(notification: ActorRef, userProfile: ActorRef, exerciseClasssifiers: ActorRef)
  extends PersistentActor with ActorLogging with AutoPassivation with UserProfileNotifications {
  import scala.concurrent.duration._

  // user reference and notifier
  private val userId = UserId(self.path.name)
  private val notificationSender = newNotificationSender(userId, notification, userProfile)

  // decoders
  private val rootSensorDataDecoder = RootSensorDataDecoder(AccelerometerDataDecoder)

  // tracing output
  private val tracing = context.actorOf(UserExercisesTracing.props, userId.toString)

  // minimum confidence
  private val confidenceThreshold = 0.5
  // how long until we stop processing
  context.setReceiveTimeout(360.seconds)
  // our unique persistenceId; the self.path.name is provided by ``UserExercises.idExtractor``,
  // hence, self.path.name is the String representation of the userId UUID.
  override val persistenceId: String = s"user-exercises-${self.path.name}"

  // chop-chop timer cancellable
  private var tooMuchRestCancellable: Option[Cancellable] = None

  import context.dispatcher

  /// decoding failed
  private def decodingFailed(error: String): Unit = {
    tracing ! DecodingFailed(error)
    sender() ! \/.left(error)
  }

  /// decoding succeeded
  private def decodedSensorData(sessionProps: SessionProps)(data: List[SensorDataWithLocation]): Unit = {
    tracing ! DecodingSucceeded(data)
    exerciseClasssifiers ! Classify(sessionProps, data)
    sender() ! \/.right(())
  }

  private def cancellingTooMuchRest(r: Receive): Receive = {
    case x if r.isDefinedAt(x) ⇒
      tooMuchRestCancellable.foreach(_.cancel())
      r(x)
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, SessionStartedEvt(sessionId, sessionProps)) ⇒
      context.become(exercising(sessionId, sessionProps))
  }

  private def exercising(id: SessionId, sessionProps: SessionProps): Receive = withPassivation(cancellingTooMuchRest {
    case ExerciseSessionStart(newSessionProps) ⇒
      val newId = SessionId.randomId()
      persist(Seq(SessionEndedEvt(id), SessionStartedEvt(newId, newSessionProps))) { x ⇒
        log.warning("ExerciseSessionStart: exercising -> exercising. Implicitly ending running session and starting a new one.")

        val (_::newSession) = x
        tracing ! newSession
        saveSnapshot(newSession)
        sender() ! \/.right(newId)
        context.become(exercising(newId, newSessionProps))
      }

    case ExerciseDataProcessSinglePacket(`id`, bits) ⇒
      log.debug("ExerciseDataProcess: exercising -> exercising.")
      tracing ! bits

      rootSensorDataDecoder
        .decodeAll(bits)
        .map(sd ⇒ List(SensorDataWithLocation(SensorDataSourceLocationAny, sd)))
        .fold(decodingFailed, decodedSensorData(sessionProps))

    case ExerciseDataProcessMultiPacket(`id`, _) ⇒
      sender() ! \/.left("Not implemented yet")

    case FullyClassifiedExercise(metadata, confidence, exercise) if confidence > confidenceThreshold ⇒
      log.debug("FullyClassifiedExercise: exercising -> exercising.")
      persist(ExerciseEvt(id, metadata, exercise)) { evt ⇒
        tracing ! evt
        tooMuchRestCancellable = Some(context.system.scheduler.scheduleOnce(sessionProps.restDuration, self, TooMuchRest))
        exercise.intensity.foreach { i ⇒
          if (i << sessionProps.intendedIntensity) notificationSender ! ScreenMessagePayload("Harder!", None, Some("default"))
          if (i >> sessionProps.intendedIntensity) notificationSender ! ScreenMessagePayload("Easier!", None, Some("default"))
        }
      }

    case Tap ⇒
      persist(ExerciseSetExplicitMarkEvt(id)) { evt ⇒
        tracing ! evt
        tooMuchRestCancellable = Some(context.system.scheduler.scheduleOnce(sessionProps.restDuration, self, TooMuchRest))
      }

    case ExerciseExplicitClassificationStart(`id`, exercise) =>
      persist(ExerciseEvt(id, ModelMetadata.user, exercise)) { evt ⇒
        tracing ! evt
      }

    case ExerciseExplicitClassificationEnd(`id`) ⇒
      self ! NoExercise(ModelMetadata.user)

    case UnclassifiedExercise(_) ⇒
      // Maybe notify the user?
      tooMuchRestCancellable = Some(context.system.scheduler.scheduleOnce(sessionProps.restDuration, self, TooMuchRest))

    case NoExercise(metadata) ⇒
      log.debug("NoExercise: exercising -> exercising.")
      persist(NoExerciseEvt(id, metadata)) { evt ⇒
        tracing ! evt
        tooMuchRestCancellable = Some(context.system.scheduler.scheduleOnce(sessionProps.restDuration, self, TooMuchRest))
      }

    case TooMuchRest ⇒
      log.debug("NoExercise: exercising -> exercising.")
      persist(TooMuchRestEvt(id)) { evt ⇒
        tracing ! evt
        notificationSender ! ScreenMessagePayload("Chop chop!", None, Some("default"))
      }

    case ExerciseSessionEnd(`id`) ⇒
      log.debug("ExerciseSessionEnd: exercising -> not exercising.")
      persist(SessionEndedEvt(id)) { evt ⇒
        tracing ! evt
        saveSnapshot(evt)
        context.become(notExercising)
        sender() ! \/.right(())
      }
  })

  private def notExercising: Receive = withPassivation {
    case ExerciseSessionStart(sessionProps) ⇒
      persist(SessionStartedEvt(SessionId.randomId(), sessionProps)) { evt ⇒
        saveSnapshot(evt)
        sender() ! \/.right(evt.sessionId)
        tracing ! evt
        context.become(exercising(evt.sessionId, sessionProps))
      }
    case ExerciseSessionEnd(_) ⇒
      sender() ! \/.left("Not in session")

    case ExerciseSessionDelete(sessionId) ⇒
      persist(SessionDeletedEvt(sessionId)) { evt ⇒
        sender() ! \/.right(())
      }
  }

  override def receiveCommand: Receive = notExercising

}
