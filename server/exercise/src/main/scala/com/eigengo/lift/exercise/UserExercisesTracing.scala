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
 * The protocol & companion
 */
object UserExercisesTracing {
  val props: Props = Props[UserExercisesTracing]

  case class DecodingSucceeded(sensorData: List[SensorDataWithLocation]) extends AnyVal
  case class DecodingFailed(error: String) extends AnyVal

  private case class Tag(tapped: Boolean, userExercise: Option[Exercise], systemExercise: Option[Exercise]) {
    def withUserExercise(ue: Option[Exercise]): Tag = copy(userExercise = ue)
    def withSystemExercise(se: Option[Exercise]): Tag = copy(systemExercise = se)
    def withToggledTap(): Tag = copy(tapped = !tapped)
  }
  private case object Tag {
    val empty = Tag(tapped = false, None, None)
  }

  def appendSensorData(id: SessionId, tag: Tag, sdwls: List[SensorDataWithLocation]): Unit = {
    def tagged(tag: Tag, s: String): String = {
      s"${tag.tapped},${tag.systemExercise.getOrElse("")},${tag.userExercise.getOrElse("")},$s"
    }

    def save(id: SessionId, tag: Tag)(sdwl: SensorDataWithLocation): Unit = {
      val fos = new FileOutputStream(s"/tmp/lift-$id-${sdwl.location}.csv", true)
      sdwl.data.foreach {
        case AccelerometerData(_, values) ⇒
          values.foreach { v ⇒
            val line = tagged(tag, s"${v.x},${v.y},${v.z}\n")
            fos.write(line.getBytes("UTF-8"))
          }
      }
      fos.close()
    }

    sdwls.foreach(save(id, tag))
  }

  def saveBits(counter: Int, id: SessionId, bits: BitVector): Int = {
    Try { val fos = new FileOutputStream(s"/tmp/lift-$id.dat", true); fos.write(bits.toByteArray); fos.close() }
    counter + 1
  }

  def saveMultiPacket(counter: Int, id: SessionId, packet: MultiPacket): Int = {
    // TODO: complete me
    ???
    counter + 1
  }

}

/**
 * Tracing actor that collects data about a running session
 */
class UserExercisesTracing extends PersistentActor {
  import com.eigengo.lift.exercise.UserExercisesTracing._
  private var counter: Int = 0
  private var tag: Tag = Tag.empty

  override def receiveRecover: Receive = Actor.emptyBehavior

  private def inASession(id: SessionId): Receive = {
    case SessionEndedEvt(`id`)         ⇒ context.become(notExercising)
    case SessionStartedEvt(newId, _)   ⇒ context.become(inASession(newId))

    case packet: BitVector             ⇒ counter = saveBits(counter, id, packet)
    case packet: MultiPacket           ⇒ counter = saveMultiPacket(counter, id, packet)
    case DecodingFailed(error)         ⇒
    case DecodingSucceeded(sensorData) ⇒ appendSensorData(id, tag, sensorData)

    case ExerciseEvt(`id`, ModelMetadata.user, exercise) ⇒ tag = tag.withUserExercise(Some(exercise))
    case ExerciseEvt(`id`, metadata, exercise) ⇒ tag = tag.withSystemExercise(Some(exercise))
    case ExerciseSetExplicitMarkEvt(`id`) ⇒ tag = tag.withToggledTap()
    case NoExercise(ModelMetadata.user) ⇒ tag = tag.withUserExercise(None)
    case NoExercise(metadata) ⇒ tag = tag.withSystemExercise(None)

    case TooMuchRestEvt(`id`) ⇒
  }

  private def notExercising: Receive = {
    case SessionStartedEvt(id, _) ⇒ tag = Tag.empty; context.become(inASession(id))
  }

  override def receiveCommand: Receive = notExercising

  override val persistenceId: String = s"user-exercises-tracing-${self.path.name}"
}
