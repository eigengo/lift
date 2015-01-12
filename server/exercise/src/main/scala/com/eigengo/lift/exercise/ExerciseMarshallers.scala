package com.eigengo.lift.exercise

import java.util.UUID

import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import com.eigengo.lift.exercise.DeviceSensorData.MultipleDeviceSensorData
import com.eigengo.lift.exercise.DeviceSensorData.MultipleDeviceSensorData.Source
import com.eigengo.lift.exercise.packet.{RawSensorData, MultiPacket}
import scodec.bits.{ByteVector, BitVector}
import spray.http.HttpRequest
import spray.httpx.marshalling.{ToResponseMarshallingContext, ToResponseMarshaller}
import spray.httpx.unmarshalling.{Deserialized, FromRequestUnmarshaller}
import spray.routing._
import spray.routing.directives.{MarshallingDirectives, PathDirectives}
import scala.collection.JavaConverters._

/**
 * Defines the marshallers for the Lift system
 */
trait ExerciseMarshallers extends MarshallingDirectives with PathDirectives with CommonPathDirectives with CommonMarshallers {

  /**
   * Unmarshals the ``HttpRequest`` to an instance of ``MultiPacket``.
   */
  implicit object BitVectorFromRequestUnmarshaller extends FromRequestUnmarshaller[MultiPacket] {
    private def convertLocation(source: Source) = {
      source match {
        case Source.WRIST => SensorDataSourceLocationWrist
        case Source.WAIST => SensorDataSourceLocationWaist
        case Source.FOOT => SensorDataSourceLocationFoot
        case Source.CHEST => SensorDataSourceLocationChest
        case _ => SensorDataSourceLocationAny
      }
    }

    override def apply(request: HttpRequest): Deserialized[MultiPacket] = {
      val bs = request.entity.data.toByteArray

      val multipleDeviceSensorData = MultipleDeviceSensorData.parseFrom(bs)

      Right(
        MultiPacket(
          multipleDeviceSensorData
            .getSingleDeviceSensorDataList().asScala
            .map(p => RawSensorData(convertLocation(p.getSource), BitVector(p.getData.toByteArray)))))
    }
  }

  val SessionIdValue: PathMatcher1[SessionId] = JavaUUID.map(SessionId.apply)
}
