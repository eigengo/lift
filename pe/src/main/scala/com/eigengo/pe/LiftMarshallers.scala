package com.eigengo.pe

import scodec.bits.BitVector
import spray.http.{ContentTypes, HttpEntity, HttpResponse, HttpRequest}
import spray.httpx.marshalling.{ToResponseMarshallingContext, ToResponseMarshaller}
import spray.httpx.unmarshalling.{Deserialized, FromRequestUnmarshaller}
import spray.routing.directives.MarshallingDirectives

/**
 * Defines the marshallers for the Lift system
 */
trait LiftMarshallers extends MarshallingDirectives {

  /**
   * Unmarshals the ``HttpRequest`` to an instance of (off-heap) ``BitVector``.
   * It is possible to have empty ``BitVector``, though this might not be particularly
   * useful for the app
   */
  implicit object BitVectorMarshaller extends FromRequestUnmarshaller[BitVector] {

    override def apply(request: HttpRequest): Deserialized[BitVector] = {
      val bs = request.entity.data.toByteString
      Right(BitVector(bs))
    }

  }

}
