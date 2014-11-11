package com.eigengo.pe.exercise

import akka.actor.{Props, Actor}
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentActor
import com.eigengo.pe.AccelerometerData
import scodec.bits.BitVector

object UserExercise {
  import Exercise._
  val shardName: String = "UserExercise"
  val props: Props = Props[UserExercise]

  val idExtractor: ShardRegion.IdExtractor = {
    case ExerciseDataCmd(userId, bits) ⇒ (userId.toString, UserExerciseDataCmd(bits))
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case cmd: Command => (math.abs(cmd.userId.hashCode()) % 100).toString
  }

  /**
   * The exercise command with the ``bits`` received from the fitness device
   * @param bits the received data
   */
  case class UserExerciseDataCmd(bits: BitVector)

  /**
   * The event with processed fitness data into ``List[AccelerometerData]``
   * @param data the accelerometer data
   */
  case class UserExerciseDataEvt(data: List[AccelerometerData])
}

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class UserExercise extends PersistentActor {
  import AccelerometerData._
  import UserExercise._

  private var buffer: BitVector = BitVector.empty

  private def validateData(data: List[AccelerometerData]): Boolean = data match {
    case Nil => true
    case h :: t => data.forall(_.samplingRate == h.samplingRate)
  }

  override val persistenceId: String = "user-exercise-persistence"

  override val receiveRecover: Receive = Actor.emptyBehavior

  override def receiveCommand: Receive = {
    case UserExerciseDataCmd(bits) =>
      val (bits2, data) = decodeAll(buffer ++ bits, Nil)
      if (validateData(data)) {
        persistAsync(UserExerciseDataEvt(data)) { e =>
          buffer = bits2
        }
      }

  }

}
