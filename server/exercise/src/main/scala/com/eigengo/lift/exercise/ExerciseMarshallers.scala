package com.eigengo.lift.exercise

import java.util.UUID

import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import scodec.bits.BitVector
import spray.http.HttpRequest
import spray.httpx.marshalling.{ToResponseMarshallingContext, ToResponseMarshaller}
import spray.httpx.unmarshalling.{Deserialized, FromRequestUnmarshaller}
import spray.routing._
import spray.routing.directives.{MarshallingDirectives, PathDirectives}

/**
 * Defines the marshallers for the Lift system
 */
trait ExerciseMarshallers extends MarshallingDirectives with PathDirectives with CommonPathDirectives with CommonMarshallers {

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

  val SessionIdValue: PathMatcher1[SessionId] = JavaUUID.map(SessionId.apply)
}
