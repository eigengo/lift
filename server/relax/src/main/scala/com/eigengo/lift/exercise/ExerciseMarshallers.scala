package com.eigengo.lift.exercise

import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import spray.http.{HttpEntity, HttpResponse, HttpRequest}
import spray.httpx.marshalling.{ToResponseMarshallingContext, ToResponseMarshaller}
import spray.httpx.unmarshalling.{Deserialized, FromRequestUnmarshaller, MalformedContent}
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

  implicit object UnitToResponseMarshaller extends ToResponseMarshaller[Unit] {
    override def apply(value: Unit, ctx: ToResponseMarshallingContext): Unit = ctx.marshalTo(HttpResponse(entity = HttpEntity("{}")))
  }

  val SessionIdValue: PathMatcher1[SessionId] = JavaUUID.map(SessionId.apply)
}
