package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor._
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import com.eigengo.pe.{AccelerometerData, actors}
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

object ExerciseProcessor {
  def props(destination: ActorRef) = Props(classOf[ExerciseProcessor], destination)
  val name = "exercise-processor"
  def lookup(implicit arf: ActorRefFactory) = actors.local.lookup(arf, name)

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param bits the submitted bits
   */
  case class ExerciseDataCmd(userId: UUID, bits: BitVector)

  /**
   * The event with processed fitness data into ``List[AccelerometerData]``
   * @param data the accelerometer data
   */
  case class ExerciseDataEvt(userId: UUID, data: AccelerometerData)
}

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class ExerciseProcessor(destination: ActorRef) extends PersistentActor with AtLeastOnceDelivery {
  import com.eigengo.pe.AccelerometerData._
  import com.eigengo.pe.exercise.ExerciseProcessor._

  private var buffer: BitVector = BitVector.empty

  private def validateData(data: List[AccelerometerData]): \/[String, AccelerometerData] = data match {
    case Nil => \/.left("Empty")
    case h :: t =>
      if (data.forall(_.samplingRate == h.samplingRate)) {
        \/.right(data.foldRight(data.last)((res, ad) => ad.copy(values = ad.values ++ res.values)))
      } else {
        \/.left("Unmatched sampling rates")
      }
  }

  override val persistenceId: String = "exercise-persistence"

  override val receiveRecover: Receive = Actor.emptyBehavior

  override def receiveCommand: Receive = {
    case ExerciseDataCmd(userId, bits) =>
      val (bits2, data) = decodeAll(buffer ++ bits, Nil)
      validateData(data).fold(
        err ⇒ sender() ! \/.left(err),
        evt ⇒ persist(ExerciseDataEvt(userId, evt)) { ad ⇒
          buffer = bits2
          sender() ! \/.right('OK)
          destination ! ad
        }
      )
  }

}
