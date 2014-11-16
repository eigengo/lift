package com.eigengo.lift.exercise

import akka.actor._
import com.eigengo.lift.common.UserId
import com.eigengo.lift.exercise.ExerciseDataProcessor.UserExerciseDataProcess
import com.eigengo.lift.exercise.UserExercises.UserExerciseDataProcessed
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

object ExerciseDataProcessor {
  def props(userExercises: ActorRef) = Props(classOf[ExerciseDataProcessor], userExercises)
  val name = "exercise-processor"

  /**
   * Receive exercise data for the given ``userId`` and the ``bits`` that may represent the exercises performed
   * @param userId the user identity
   * @param sessionId the session identity               
   * @param bits the submitted bits
   */
  case class UserExerciseDataProcess(userId: UserId, sessionId: SessionId, bits: BitVector)
}

/**
 * Processes the exercise data commands by parsing the bits and then generating the
 * appropriate events.
 */
class ExerciseDataProcessor(userExercises: ActorRef) extends Actor {
  import com.eigengo.lift.exercise.AccelerometerData._
  import scala.concurrent.duration._

  context.setReceiveTimeout(120.seconds)

  private def validateData(data: List[AccelerometerData]): \/[String, AccelerometerData] = data match {
    case Nil => \/.left("Empty")
    case h :: t =>
      if (data.forall(_.samplingRate == h.samplingRate)) {
        \/.right(data.foldRight(data.last)((res, ad) => ad.copy(values = ad.values ++ res.values)))
      } else {
        \/.left("Unmatched sampling rates")
      }
  }

  override def receive: Receive = {
    case UserExerciseDataProcess(userId, sessionId, bits) ⇒
      val (BitVector.empty, data) = decodeAll(bits, Nil)
      validateData(data).fold(
        { err ⇒ sender() ! \/.left(err) },
        { ad ⇒
          sender() ! \/.right('OK)
          userExercises ! UserExerciseDataProcessed(userId, sessionId, ad)
        }
      )
  }

}
