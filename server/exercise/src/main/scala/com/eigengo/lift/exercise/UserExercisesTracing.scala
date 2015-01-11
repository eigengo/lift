package com.eigengo.lift.exercise

import java.io.FileOutputStream

import akka.actor.Actor.Receive
import akka.actor.Props
import akka.persistence.{PersistentActor, PersistentView}
import com.eigengo.lift.exercise.packet.MultiPacket
import scodec.bits.BitVector

import scala.util.Try

class UserExercisesTracing extends PersistentActor {
  import UserExercisesTracing._

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case ReceivedSinglePacket(id, bits) ⇒
      // Tracing code: save any input chunk to an arbitrarily-named file for future analysis.
      // Ideally, this will go somewhere more durable, but this is sufficient for now.
      UserExercisesTracing.saveBits(id, bits)

  }

  override def persistenceId: String = ???
}

object UserExercisesTracing {
  val props: Props = Props[UserExercisesTracing]

  case class ReceivedSinglePacket(id: SessionId, bits: BitVector)
  case class ReceivedMultiPacket(id: SessionId, mp: MultiPacket)
  case class DecodingSucceeded(id: SessionId, sensorData: List[SensorDataWithLocation])
  case class DecodingFailed(id: SessionId, error: String)

  def saveAccelerometerData(id: SessionId, datas: List[AccelerometerData]) = {
    Try {
      val fos = new FileOutputStream(s"/tmp/ad-$id.csv", true)
      datas.foreach { ad ⇒
        ad.values.foreach { v ⇒
          val line = s"${v.x},${v.y},${v.z}\n"
          fos.write(line.getBytes("UTF-8"))
        }
      }
      fos.close()
    }
  }


  def saveBits(id: SessionId, bits: BitVector): Unit = {
    Try { val fos = new FileOutputStream(s"/tmp/ad-$id.dat", true); fos.write(bits.toByteArray); fos.close() }
  }

}
