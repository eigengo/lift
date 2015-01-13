package com.eigengo.lift.exercise

import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import scodec.bits.BitVector
import spray.http.HttpRequest
import spray.httpx.unmarshalling.{MalformedContent, Deserialized, FromRequestUnmarshaller}
import spray.routing._
import spray.routing.directives.{MarshallingDirectives, PathDirectives}

/**
 * Defines the marshallers for the Lift system
 */
trait ExerciseMarshallers extends MarshallingDirectives with PathDirectives with CommonPathDirectives with CommonMarshallers {

  implicit object MultiPacketFromRequestUnmarshaller extends FromRequestUnmarshaller[MultiPacket] {
    override def apply(request: HttpRequest): Deserialized[MultiPacket] = {
      MultiPacketDecoder.decode(request.entity.data.toByteString.asByteBuffer).fold(x ⇒ Left(MalformedContent(x)), Right.apply)
    }
  }

  val SessionIdValue: PathMatcher1[SessionId] = JavaUUID.map(SessionId.apply)
}
