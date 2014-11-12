package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentActor
import com.eigengo.pe.{AccelerometerData, actors}
import scodec.bits.BitVector

object Exercise {
  val shardName: String = "exercise-shard"
  val props: Props = Props[Exercise]

  def lookup(implicit arf: ActorRefFactory): ActorRef = actors.shard.lookup(arf, shardName)

  val idExtractor: ShardRegion.IdExtractor = {
    case cmd@ExerciseDataCmd(userId, bits) ⇒ (userId.toString, cmd)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case ExerciseDataCmd(userId, _) => (math.abs(userId.hashCode()) % 100).toString
  }

  /**
   *
   * @param userId
   * @param bits
   */
  case class ExerciseDataCmd(userId: UUID, bits: BitVector)

}

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class Exercise extends PersistentActor {
  import com.eigengo.pe.AccelerometerData._
  import com.eigengo.pe.exercise.Exercise._
  import com.eigengo.pe.exercise.ExerciseView._

  private var buffer: BitVector = BitVector.empty

  private def validateData(data: List[AccelerometerData]): Boolean = data match {
    case Nil => true
    case h :: t => data.forall(_.samplingRate == h.samplingRate)
  }

  override val persistenceId: String = "user-exercise-persistence"

  override val receiveRecover: Receive = Actor.emptyBehavior

  override def receiveCommand: Receive = {
    case ExerciseDataCmd(userId, bits) =>
      val (bits2, data) = decodeAll(buffer ++ bits, Nil)
      if (validateData(data)) {
        persist(ExerciseDataEvt(userId, data)) { e =>
          buffer = bits2
        }
      }

  }

}
