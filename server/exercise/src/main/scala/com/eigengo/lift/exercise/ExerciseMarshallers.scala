package com.eigengo.lift.exercise

import java.util.UUID

import com.eigengo.lift.profile.UserProfileProtocol.UserId
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{CustomSerializer, DefaultFormats, Formats}
import scodec.bits.BitVector
import spray.http.HttpRequest
import spray.httpx.Json4sSupport
import spray.httpx.unmarshalling.{Deserialized, FromRequestUnmarshaller}
import spray.routing._
import spray.routing.directives.{MarshallingDirectives, PathDirectives}

/**
 * Defines the marshallers for the Lift system
 */
trait ExerciseMarshallers extends MarshallingDirectives with PathDirectives with Json4sSupport {

  case object UUIDSerialiser extends CustomSerializer[UUID](format => (
    {
      case JString(s) => UUID.fromString(s)
      case JNull => null
    },
    {
      case x: UUID => JString(x.toString)
    }
    )
  )
  override implicit def json4sFormats: Formats = DefaultFormats + UUIDSerialiser

  /**
   * Unmarshals the ``HttpRequest`` to an instance of (off-heap) ``BitVector``.
   * It is possible to have empty ``BitVector``, though this might not be particularly
   * useful for the app
   */
  implicit object BitVectorFromRequestUnmarshaller extends FromRequestUnmarshaller[BitVector] {

    override def apply(request: HttpRequest): Deserialized[BitVector] = {
      val bs = request.entity.data.toByteString
      Right(BitVector(bs))
    }

  }

  val UserIdValue: PathMatcher1[UserId] = JavaUUID.map(UserId.apply)
  val SessionIdValue: PathMatcher1[SessionId] = JavaUUID.map(SessionId.apply)
}
