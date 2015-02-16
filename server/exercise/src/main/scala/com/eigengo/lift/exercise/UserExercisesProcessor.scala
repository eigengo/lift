package com.eigengo.lift.exercise

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.SnapshotOffer
import com.eigengo.lift.common.{AutoPassivation, UserId}
import java.io.FileOutputStream
import com.eigengo.lift.exercise.classifiers.ExerciseModelChecking
import scala.language.postfixOps
import scalaz.\/

/**
 * User + list of exercises companion
 */
object UserExercisesProcessor {

  /** The shard name */
  val shardName = "user-exercises"
  /** The sessionProps to create the actor on a node */
  def props(kafka: ActorRef, notification: ActorRef, userProfile: ActorRef) = Props(classOf[UserExercisesProcessor], kafka, notification, userProfile)

  /**
   * Remove a session identified by ``sessionId`` for user identified by ``userId``
   * @param userId the user identity
   * @param sessionId the session identity
   */
  case class UserExerciseSessionDelete(userId: UserId, sessionId: SessionId)

  /**
   * Abandons the session identified by ``userId`` and ``sessionId``
   * @param userId the user identity
   * @param sessionId the session identity
   */
  case class UserExerciseSessionAbandon(userId: UserId, sessionId: SessionId)

  /**
   * Replay all data received for a possibly existing ``sessionId`` for the given ``userId``
   * @param userId the user identity
   * @param sessionId the session identity
   * @param data all session data
   */
  case class UserExerciseSessionReplayProcessData(userId: UserId, sessionId: SessionId, data: Array[Byte])

  /**
   * Starts the replay all data received for a possibly existing ``sessionId`` for the given ``userId``
   * @param userId the user identity
   * @param sessionId the session identity
   * @param sessionProps the session props
   */
  case class UserExerciseSessionReplayStart(userId: UserId, sessionId: SessionId, sessionProps: SessionProperties)

  /**
   * User classified exercise start.
   * @param userId user
   * @param sessionId session
   * @param exercise the exercise
   */
  case class UserExerciseExplicitClassificationStart(userId: UserId, sessionId: SessionId, exercise: Exercise)

  /**
   * Sets the metric of all the exercises in the current set that don't have a metric yet. So, suppose the user is in
   * a set doing squats, and the system's events are
   *
   * - ExerciseEvt(squat) // without metric
   * - ExerciseEvt(squat) // without metric
   * - ExerciseEvt(squat) // without metric
   * *** UserExerciseSetExerciseMetric
   *
   * The system generates the ExerciseMetricSetEvt, and the views should add the metric to the previous three exercises
   *
   * @param userId the user identity
   * @param sessionId the session identity
   * @param metric the received metric
   */
  case class UserExerciseSetExerciseMetric(userId: UserId, sessionId: SessionId, metric: Metric)

  /**
   * Obtains a list of example exercises for the given session
   * @param userId the user
   * @param sessionId the session
   */
  case class UserExerciseExplicitClassificationExamples(userId: UserId, sessionId: SessionId)

  /**
   * User classification end
   * @param userId the user
   * @param sesionId the session
   */
  case class UserExerciseExplicitClassificationEnd(userId: UserId, sesionId: SessionId)

  /**
   * Receive multiple packets of data for the given ``userId`` and ``sessionId``. The ``packets`` is a ZIP archive
   * containing multiple files, each representing a single packet.
   *
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
   * Obtain list of classification examples
   * @param sessionId the session
   */
  private case class ExerciseExplicitClassificationExamples(sessionId: SessionId)

  /**
   * Replay the ``sessionId`` by re-processing all data in ``data``
   * @param sessionId the session identity
   * @param data all session data
   */
  private case class ExerciseSessionReplayProcessData(sessionId: SessionId, data: Array[Byte])

  /**
   * Starts the replay the ``sessionId`` by re-processing all data in ``data``
   * @param sessionId the session identity
   * @param sessionProps the session props
   */
  private case class ExerciseSessionReplayStart(sessionId: SessionId, sessionProps: SessionProperties)

  /**
   * Sets the metric for the unmarked exercises in the currently open set
   * @param sessionId the session identity
   * @param metric the metric
   */
  private case class ExerciseSetExerciseMetric(sessionId: SessionId, metric: Metric)

  /**
   * User classified exercise.
   * @param sessionId session
   */
  private case class ExerciseExplicitClassificationEnd(sessionId: SessionId)

  /**
   * Abandons the give exercise session
   * @param sessionId the session identity
   */
  private case class ExerciseSessionAbandon(sessionId: SessionId)

  /**
   * Starts the user exercise sessionProps
   * @param userId the user identity
   * @param sessionProps the sessionProps details
   */
  case class UserExerciseSessionStart(userId: UserId, sessionProps: SessionProperties)

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
  private case class ExerciseSessionStart(sessionProps: SessionProperties)

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
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case UserExerciseSessionStart(userId, session)                            ⇒ (userId.toString, ExerciseSessionStart(session))
    case UserExerciseSessionEnd(userId, sessionId)                            ⇒ (userId.toString, ExerciseSessionEnd(sessionId))
    case UserExerciseDataProcessMultiPacket(userId, sessionId, packets)       ⇒ (userId.toString, ExerciseDataProcessMultiPacket(sessionId, packets))
    case UserExerciseSessionDelete(userId, sessionId)                         ⇒ (userId.toString, ExerciseSessionDelete(sessionId))
    case UserExerciseSessionAbandon(userId, sessionId)                        ⇒ (userId.toString, ExerciseSessionAbandon(sessionId))
    case UserExerciseSessionReplayProcessData(userId, sessionId, data)        ⇒ (userId.toString, ExerciseSessionReplayProcessData(sessionId, data))
    case UserExerciseSessionReplayStart(userId, sessionId, props)             ⇒ (userId.toString, ExerciseSessionReplayStart(sessionId, props))
    case UserExerciseExplicitClassificationStart(userId, sessionId, exercise) ⇒ (userId.toString, ExerciseExplicitClassificationStart(sessionId, exercise))
    case UserExerciseExplicitClassificationEnd(userId, sessionId)             ⇒ (userId.toString, ExerciseExplicitClassificationEnd(sessionId))
    case UserExerciseExplicitClassificationExamples(userId, sessionId)        ⇒ (userId.toString, ExerciseExplicitClassificationExamples(sessionId))
    case UserExerciseSetExerciseMetric(userId, sessionId, metric)             ⇒ (userId.toString, ExerciseExplicitClassificationExamples(sessionId))

  }

  /**
   * Resolves the shard name from the incoming message.
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserExerciseSessionStart(userId, _)                   ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionEnd(userId, _)                     ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseDataProcessMultiPacket(userId, _, _)      ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionDelete(userId, _)                  ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionAbandon(userId, _)                 ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseExplicitClassificationStart(userId, _, _) ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseExplicitClassificationEnd(userId, _)      ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseExplicitClassificationExamples(userId, _) ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionReplayProcessData(userId, _, _)    ⇒ s"${userId.hashCode() % 10}"
    case UserExerciseSessionReplayStart(userId, _, _)          ⇒ s"${userId.hashCode() % 10}"
  }
}

/**
 * Processes each user's exercise sessions. It contains three main states:
 *
 * - not exercising
 * - exercising
 * - replaying
 *
 * The sunny day flow is that this actor moves from not exercising to exercising, receives all
 * sensor data from the client, and then moves back to not exercising.
 *
 * This actor can instruct the mobile to switch to offline session mode—that is to stop sending
 * any sensor data, and prepare to send all sensor data in one block in a replay request—if it
 * notices serious inconsistency in the received data.
 *
 * Finally, the client can replay an offline session by sending all sensor data in one request
 * and this instance must process them as though it received the data by parts as part of an online
 * session.
 */
class UserExercisesProcessor(override val kafka: ActorRef, notification: ActorRef, userProfile: ActorRef)
  extends KafkaProducerPersistentActor
  with ExerciseModelChecking
  with ActorLogging
  with AutoPassivation {

  import com.eigengo.lift.exercise.UserExercises._
  import com.eigengo.lift.exercise.UserExercisesClassifier._
  import com.eigengo.lift.exercise.UserExercisesProcessor._
  import scala.concurrent.duration._

  // user reference and notifier
  private val userId = UserId(self.path.name)

  // decoders
  private val rootSensorDataDecoder = RootSensorDataDecoder(AccelerometerDataDecoder, RotationDataDecoder)

  // tracing output
  /*private val tracing = */context.actorOf(UserExercisesTracing.props(userId))

  // how long until we stop processing
  context.setReceiveTimeout(360.seconds)
  // our unique persistenceId; the self.path.name is provided by ``UserExercises.idExtractor``,
  // hence, self.path.name is the String representation of the userId UUID.
  override val persistenceId: String = s"user-exercises-${userId.toString}"

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, SessionStartedEvt(sessionId, sessionProps)) ⇒
      registerModelChecking(sessionProps)
      context.become(exercising(sessionId, sessionProps))
  }

  private def replaying(oldSessionId: SessionId, newSessionId: SessionId, sessionProps: SessionProperties): Receive = withPassivation {
    case ExerciseSessionReplayStart(_, _) ⇒
      sender() ! \/.left("Another session replay in progress")

    case ExerciseSessionReplayProcessData(`newSessionId`, data) ⇒
      persist(Seq(SessionAbandonedEvt(oldSessionId), SessionStartedEvt(newSessionId, sessionProps))) { case _ :: started :: Nil ⇒
        log.warning("Not yet handling processing of the data")
        sender() ! \/.right(())
      }

      // TODO: Implement proper replay handling. For now, we save the file and end the session.
      val fos = new FileOutputStream(s"target/$newSessionId.mp")
      fos.write(data)
      fos.close()

      persist(SessionEndedEvt(newSessionId)) { _ ⇒
        log.info("ExerciseSessionReplayProcessData: replaying -> not exercising")
        unregisterModelChecking()
        context.become(notExercising)
      }

    case x ⇒
      log.warning(s"Unexpected $x in replaying")
      sender() ! \/.left(s"Unexpected in replaying $x")
  }

  private def exercising(id: SessionId, sessionProps: SessionProperties): Receive = withPassivation {
    // start and end
    case ExerciseSessionStart(newSessionProps) ⇒
      val newId = SessionId.randomId()
      persist(Seq(SessionEndedEvt(id), SessionStartedEvt(newId, newSessionProps))) { case (_::newSession::Nil) ⇒
        log.warning("ExerciseSessionStart: exercising -> exercising. Implicitly ending running session and starting a new one.")

        saveSnapshot(newSession)
        sender() ! \/.right(newId)
        registerModelChecking(newSessionProps)
        context.become(exercising(newId, newSessionProps))
      }

    case ExerciseSessionEnd(`id`) ⇒
      log.info("ExerciseSessionEnd: exercising -> not exercising.")
      persist(SessionEndedEvt(id)) { evt ⇒
        saveSnapshot(evt)
        unregisterModelChecking()
        context.become(notExercising)
        sender() ! \/.right(())
      }

    case ExerciseSessionAbandon(`id`) ⇒
      log.info("ExerciseSessionEnd: exercising -> not exercising.")
      persist(SessionAbandonedEvt(id)) { evt ⇒
        saveSnapshot(evt)
        unregisterModelChecking()
        context.become(notExercising)
        sender() ! \/.right(())
      }

    // packet from the mobile / wearables
    case ExerciseDataProcessMultiPacket(`id`, packet) ⇒
      log.info("ExerciseDataProcess: exercising -> exercising.")

      if (classifier.isEmpty) {
        sender() ! \/.left("Attempted to classify multi-packet exercising data when no classifier was defined!")
      } else {
        val (h::t) = packet.packets.map { pwl ⇒
          rootSensorDataDecoder
            .decodeAll(pwl.payload)
            .map(sd ⇒ SensorDataWithLocation(pwl.sourceLocation, sd))
        }
        val result = t.foldLeft(h.map(x ⇒ List(x)))((r, b) ⇒ r.flatMap(sdwls ⇒ b.map(x ⇒ x :: sdwls)))
        result.fold({ err ⇒ persist(MultiPacketDecodingFailedEvt(id, err, packet)) { evt ⇒ sender() ! \/.left(err) } },
                    { dec ⇒ persist(ClassifyExerciseEvt(sessionProps, dec)) { evt ⇒ classifier.foreach(ref => { ref ! evt; sender() ! \/.right(()) }) } })
      }

    // explicit classification
    case ExerciseExplicitClassificationStart(`id`, exercise) =>
      persistAndProduceToKafka(ExerciseEvt(id, ModelMetadata.user, exercise)) { evt ⇒ }

    case ExerciseExplicitClassificationEnd(`id`) ⇒
      self ! NoExercise(ModelMetadata.user)

    case ExerciseExplicitClassificationExamples(`id`) ⇒
      classifier.foreach(_.tell(ClassificationExamples(sessionProps), sender()))

    // explicit metrics
    case ExerciseSetExerciseMetric(`id`, metric) ⇒
      persist(ExerciseSetExerciseMetricEvt(id, metric)) { evt ⇒ }

    // classification results
    case FullyClassifiedExercise(metadata, confidence, exercise) ⇒
      log.info("FullyClassifiedExercise: exercising -> exercising.")
      persist(ExerciseEvt(id, metadata, exercise)) { evt ⇒ }

    case Tap ⇒
      persist(ExerciseSetExplicitMarkEvt(id)) { evt ⇒ }

    case UnclassifiedExercise(_) ⇒

    case NoExercise(metadata) ⇒
      persist(NoExerciseEvt(id, metadata)) { evt ⇒ }

    case x ⇒
      log.warning(s"Unexpected $x in exercising")
      sender() ! \/.left(s"Unexpected in exercising $x")
  }

  private def notExercising: Receive = withPassivation {
    case ExerciseSessionStart(sessionProps) ⇒
      log.info(s"ExerciseSessionStart: not exercising -> exercising")
      persist(SessionStartedEvt(SessionId.randomId(), sessionProps)) { evt ⇒
        saveSnapshot(evt)
        sender() ! \/.right(evt.sessionId)
        log.info(s"-> exercising(${evt.sessionId})")
        registerModelChecking(sessionProps)
        context.become(exercising(evt.sessionId, sessionProps))
      }

    case ExerciseSessionReplayStart(oldSessionId, sessionProps) ⇒
      val newSessionId = SessionId.randomId()
      sender() ! \/.right(newSessionId)
      log.info(s"ExerciseSessionReplayStart: not exercising -> replaying($newSessionId)")
      unregisterModelChecking()
      context.become(replaying(oldSessionId, newSessionId, sessionProps))

    case ExerciseSessionEnd(_) ⇒
      sender() ! \/.left("Not in session")

    case ExerciseSessionDelete(sessionId) ⇒
      persistAndProduceToKafka(SessionDeletedEvt(sessionId)) { evt ⇒
        sender() ! \/.right(())
      }

    case x ⇒
      log.warning(s"Unexpected $x in not exercising")
      sender() ! \/.left(s"Unexpected in not exercising $x")
  }

  override def receiveCommand: Receive = notExercising
}
