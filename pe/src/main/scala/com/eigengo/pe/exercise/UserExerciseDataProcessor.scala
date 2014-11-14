package com.eigengo.pe.exercise

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, SnapshotOffer}
import com.eigengo.pe.{AccelerometerData, Actors, SessionId, UserId}
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

object UserExerciseDataProcessor {
  def props(userExercises: ActorRef) = Props(classOf[UserExerciseDataProcessor], userExercises)
  val shardName = "user-exercise-processor"
  def lookup(implicit arf: ActorRefFactory) = Actors.shard.lookup(arf, shardName)

  /**
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case UserExerciseDataCmd(userId, sessionId, bits) ⇒ (userId.toString, ExerciseDataCmd(sessionId, bits))
  }

  /**
   * Resolves the shard name from the incoming message. 
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case UserExerciseDataCmd(userId, _) ⇒ "global"
  }

  /**
   * Exercise data for the given session and bits
   * @param sessionId the session id
   * @param bits the bits
   */
  private case class ExerciseDataCmd(sessionId: SessionId, bits: BitVector)

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param sessionId the session identity               
   * @param bits the submitted bits
   */
  case class UserExerciseDataCmd(userId: UserId, sessionId: SessionId, bits: BitVector)
}

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class UserExerciseDataProcessor(userExercises: ActorRef) extends PersistentActor with AtLeastOnceDelivery {
  import akka.contrib.pattern.ShardRegion.Passivate
  import com.eigengo.pe.AccelerometerData._
  import com.eigengo.pe.exercise.UserExerciseDataProcessor._
  import com.eigengo.pe.exercise.UserExercises._
  import scala.concurrent.duration._

  context.setReceiveTimeout(120.seconds)
  private var buffer: BitVector = BitVector.empty
  private val userId: UserId = UserId(self.path.name)

  private def validateData(data: List[AccelerometerData]): \/[String, AccelerometerData] = data match {
    case Nil => \/.left("Empty")
    case h :: t =>
      if (data.forall(_.samplingRate == h.samplingRate)) {
        \/.right(data.foldRight(data.last)((res, ad) => ad.copy(values = ad.values ++ res.values)))
      } else {
        \/.left("Unmatched sampling rates")
      }
  }

  override val persistenceId: String = "user-exercise-persistence"

  override val receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: BitVector) ⇒
      buffer = snapshot
  }

  override def receiveCommand: Receive = {
    // passivation support
    case ReceiveTimeout ⇒
      context.parent ! Passivate(stopMessage = 'stop)
    case 'stop ⇒
      context.stop(self)

    case ExerciseDataCmd(sessionId, bits) ⇒
      val (bits2, data) = decodeAll(buffer ++ bits, Nil)
      validateData(data).fold(
        err ⇒ sender() ! \/.left(err),
        evt ⇒ persist(UserExerciseDataEvt(userId, sessionId, evt)) { ad ⇒
          buffer = bits2
          saveSnapshot(buffer)
          sender() ! \/.right('OK)
          userExercises ! ad
        }
      )
  }

}
