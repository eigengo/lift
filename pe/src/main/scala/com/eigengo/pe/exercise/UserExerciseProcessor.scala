package com.eigengo.pe.exercise

import akka.actor.Actor
import akka.persistence.PersistentActor
import com.eigengo.pe.AccelerometerData
import scodec.bits.BitVector

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class UserExerciseProcessor extends PersistentActor {
  import AccelerometerData._
  import UserExerciseProtocol._

  private var buffer: BitVector = BitVector.empty

  private def validateData(data: List[AccelerometerData]): Boolean = data match {
    case Nil => true
    case h :: t => data.forall(_.samplingRate == h.samplingRate)
  }

  override val persistenceId: String = "user-exercise-persistence"

  override val receiveRecover: Receive = Actor.emptyBehavior

  override def receiveCommand: Receive = {
    case ExerciseDataCmd(bits) =>
      val (bits2, data) = decodeAll(buffer ++ bits, Nil)
      if (validateData(data)) {
        persistAsync(ExerciseDataEvt(data)) { e =>
          buffer = bits2
        }
      }

  }

}
