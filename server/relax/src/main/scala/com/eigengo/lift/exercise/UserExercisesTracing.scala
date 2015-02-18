package com.eigengo.lift.exercise

import java.io.FileOutputStream

import akka.actor.Props
import akka.persistence.PersistentView
import com.eigengo.lift.common.UserId
import com.eigengo.lift.exercise.UserExercisesClassifier._

import scala.util.Try

/**
 * The protocol & companion
 */
object UserExercisesTracing {
  def props(userId: UserId): Props = Props(classOf[UserExercisesTracing], userId.toString)

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
        case RotationData(_, values) ⇒
          values.foreach { v ⇒
            val line = tagged(tag, s"${v.x},${v.y},${v.z}\n")
            fos.write(line.getBytes("UTF-8"))
          }

      }
      fos.close()
    }

    sdwls.foreach(save(id, tag))
  }

  def saveMultiPacket(counter: Int, id: SessionId, packet: MultiPacket): Int = {
    packet.packets.foreach { pwl ⇒
      Try { val fos = new FileOutputStream(s"/tmp/lift-$id-${pwl.sourceLocation}-$counter.dat", true); fos.write(pwl.payload.toByteArray); fos.close() }
    }
    counter + 1
  }

}

/**
 * Tracing actor that collects data about a running session
 */
class UserExercisesTracing(userId: String) extends PersistentView {
  import com.eigengo.lift.exercise.UserExercises._
  import com.eigengo.lift.exercise.UserExercisesTracing._

  private var counter: Int = 0
  private var tag: Tag = Tag.empty

  private def inASession(id: SessionId): Receive = {
    case SessionEndedEvt(`id`)       ⇒ context.become(notExercising)
    case SessionStartedEvt(newId, _) ⇒ context.become(inASession(newId))

    case MultiPacketDecodingFailedEvt(_, _, packet)  ⇒ counter = saveMultiPacket(counter, id, packet)

    case ClassifyExerciseEvt(_, sdwl)                ⇒ appendSensorData(id, tag, sdwl)

    case ExerciseEvt(`id`, ModelMetadata.user, exercise) ⇒ tag = tag.withUserExercise(Some(exercise))
    case ExerciseEvt(`id`, metadata, exercise)           ⇒ tag = tag.withSystemExercise(Some(exercise))
    case ExerciseSetExplicitMarkEvt(`id`)                ⇒ tag = tag.withToggledTap()
    case NoExercise(ModelMetadata.user)                  ⇒ tag = tag.withUserExercise(None)
    case NoExercise(metadata)                            ⇒ tag = tag.withSystemExercise(None)
  }

  private def notExercising: Receive = {
    case SessionStartedEvt(id, _) ⇒ tag = Tag.empty; context.become(inASession(id))
  }

  override def receive: Receive = notExercising

  override val persistenceId: String = s"user-exercises-${userId.toString}"

  override val viewId: String = s"user-exercises-tracing-${userId.toString}"
}
