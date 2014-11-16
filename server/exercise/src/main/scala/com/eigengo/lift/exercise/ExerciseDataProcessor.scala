package com.eigengo.lift.exercise

import akka.actor._
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.Actors
import com.eigengo.lift.exercise.ExerciseDataProcessor.ExerciseDataCmd
import com.eigengo.lift.exercise.UserExercises.{SessionId, UserExerciseDataEvt}
import com.eigengo.lift.profile.UserProfileProtocol.UserId
import scodec.bits.BitVector

import scala.language.postfixOps
import scalaz.\/

object ExerciseDataProcessor {
  def props(userExercises: ActorRef) = Props(classOf[ExerciseDataProcessor], userExercises)
  val name = "exercise-processor"
  def lookup(implicit arf: ActorRefFactory) = Actors.local.lookup(arf, name)

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
class ExerciseDataProcessor(userExercises: ActorRef) extends PersistentActor {
  import akka.contrib.pattern.ShardRegion.Passivate
  import scala.concurrent.duration._
  import AccelerometerData._

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

  override val persistenceId: String = s"user-exercise-persistence-${self.path.name}"

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
