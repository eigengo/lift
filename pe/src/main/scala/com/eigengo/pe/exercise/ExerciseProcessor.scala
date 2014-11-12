package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentActor
import com.eigengo.pe.{AccelerometerData, actors}
import scodec.bits.BitVector

object ExerciseProcessor {
  val props = Props[ExerciseProcessor]
  val name = "exercise-processor"
  def lookup(implicit arf: ActorRefFactory) = actors.local.lookup(arf, name)

  val idExtractor: ShardRegion.IdExtractor = {
    case cmd@ExerciseDataCmd(userId, bits) ⇒ (userId.toString, cmd)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case ExerciseDataCmd(userId, _) => (math.abs(userId.hashCode()) % 100).toString
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
class ExerciseProcessor extends PersistentActor {
  import com.eigengo.pe.AccelerometerData._
  import com.eigengo.pe.exercise.ExerciseProcessor._
  import com.eigengo.pe.exercise.ExerciseView._

  private var buffer: BitVector = BitVector.empty

  private def validateData(data: List[AccelerometerData]): Boolean = data match {
    case Nil => true
    case h :: t => data.forall(_.samplingRate == h.samplingRate)
  }

  override val persistenceId: String = "exercise-persistence"

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
