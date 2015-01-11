package com.eigengo.lift.exercise

import java.io.FileOutputStream

import akka.actor.{Actor, Props}
import akka.persistence.PersistentActor
import com.eigengo.lift.exercise.ExerciseClassifier._
import com.eigengo.lift.exercise.UserExercisesView._
import com.eigengo.lift.exercise.packet.MultiPacket
import scodec.bits.BitVector

import scala.util.Try

/**
 * Tracing actor that collects data about a running session
 */
class UserExercisesTracing extends PersistentActor {
  import com.eigengo.lift.exercise.UserExercisesTracing._
  private var tapped: Boolean = false
  private var currentUserExercise: Option[Exercise] = None
  private var currentSystemExercise: Option[Exercise] = None

  override def receiveRecover: Receive = Actor.emptyBehavior

  private def inASession(id: SessionId): Receive = {
    case SessionEndedEvt(`id`)         ⇒ context.become(notExercising)
    case SessionStartedEvt(newId, _)   ⇒ context.become(inASession(newId))

    case packet: BitVector             ⇒ UserExercisesTracing.saveBits(id, packet)
    case packet: MultiPacket           ⇒ // TODO: complete me
    case DecodingFailed(error)         ⇒
    case DecodingSucceeded(sensorData) ⇒ sensorData.foreach { sd ⇒
        sd.data.foreach {
          case AccelerometerData(_, values) ⇒ saveAccelerometerData(id, values)
        }
      }

    case ExerciseEvt(`id`, ModelMetadata.user, exercise) ⇒
    case ExerciseEvt(`id`, metadata, exercise) ⇒
    case ExerciseSetExplicitMarkEvt(`id`) ⇒
    case NoExercise(ModelMetadata.user) ⇒
    case NoExercise(metadata) ⇒

    case TooMuchRestEvt(`id`) ⇒
  }

  private def notExercising: Receive = {
    case SessionStartedEvt(id, _) ⇒ context.become(inASession(id))
  }

  override def receiveCommand: Receive = notExercising

  override val persistenceId: String = s"user-exercises-tracing-${self.path.name}"
}

/**
 * The protocol & companion
 */
object UserExercisesTracing {
  val props: Props = Props[UserExercisesTracing]

  case class DecodingSucceeded(sensorData: List[SensorDataWithLocation]) extends AnyVal
  case class DecodingFailed(error: String) extends AnyVal


  def saveAccelerometerData(id: SessionId, values: List[AccelerometerValue]) = {
    Try {
      val fos = new FileOutputStream(s"/tmp/ad-$id.csv", true)
      values.foreach { v ⇒
          val line = s"${v.x},${v.y},${v.z}\n"
          fos.write(line.getBytes("UTF-8"))
        }
      fos.close()
    }
  }


  def saveBits(id: SessionId, bits: BitVector): Unit = {
    Try { val fos = new FileOutputStream(s"/tmp/ad-$id.dat", true); fos.write(bits.toByteArray); fos.close() }
  }

}
