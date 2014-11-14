package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.{SnapshotOffer, AtLeastOnceDelivery, PersistentActor}
import com.eigengo.pe.{AccelerometerData, actors}
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

object UserExerciseProcessor {
  def props(userExercises: ActorRef) = Props(classOf[UserExerciseProcessor], userExercises)
  val shardName = "user-exercise-processor"
  def lookup(implicit arf: ActorRefFactory) = actors.shard.lookup(arf, shardName)

  /**
   * Extracts the identity of the shard from the messages sent to the coordinator. We have per-user shard,
   * so our identity is ``userId.toString``
   */
  val idExtractor: ShardRegion.IdExtractor = {
    case ExerciseDataCmd(userId, bits) ⇒ (userId.toString, bits)
  }

  /**
   * Resolves the shard name from the incoming message. 
   */
  val shardResolver: ShardRegion.ShardResolver = {
    case ExerciseDataCmd(userId, _) ⇒ "global"
  }

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param bits the submitted bits
   */
  case class ExerciseDataCmd(userId: UUID, bits: BitVector)
}

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class UserExerciseProcessor(userExercises: ActorRef) extends PersistentActor with AtLeastOnceDelivery {
  import com.eigengo.pe.AccelerometerData._
  import com.eigengo.pe.exercise.UserExercises._
  import scala.concurrent.duration._
  import ShardRegion.Passivate

  context.setReceiveTimeout(120.seconds)
  private var buffer: BitVector = BitVector.empty
  private val userId: UUID = UUID.fromString(self.path.name)

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

    case bits: BitVector ⇒
      val (bits2, data) = decodeAll(buffer ++ bits, Nil)
      validateData(data).fold(
        err ⇒ sender() ! \/.left(err),
        evt ⇒ persist(ExerciseDataEvt(userId, evt)) { ad ⇒
          buffer = bits2
          saveSnapshot(buffer)
          sender() ! \/.right('OK)
          userExercises ! ad
        }
      )
  }

}
