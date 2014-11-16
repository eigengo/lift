package com.eigengo.lift.exercise

import scodec.bits.BitVector
import spray.http.{ContentTypes, HttpEntity}
import spray.httpx.marshalling.{Marshaller, MarshallingContext}
import spray.routing.directives.MarshallingDirectives

trait LiftTestMarshallers extends MarshallingDirectives {

  implicit object BitVectorMarshaller extends Marshaller[BitVector] {
    override def apply(value: BitVector, ctx: MarshallingContext): Unit = {
      ctx.marshalTo(HttpEntity(ContentTypes.`application/octet-stream`, value.toByteArray))
    }
  }

}
