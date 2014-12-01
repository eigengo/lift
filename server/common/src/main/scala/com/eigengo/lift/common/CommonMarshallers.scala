package com.eigengo.lift.common

import java.text.SimpleDateFormat
import java.util.UUID

import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{Serializer, CustomSerializer, DefaultFormats, Formats}
import spray.http.StatusCodes
import spray.httpx.Json4sSupport
import spray.httpx.marshalling.{ToResponseMarshallingContext, ToResponseMarshaller}
import spray.routing.directives.MarshallingDirectives

import scala.concurrent.Future
import scalaz.{-\/, \/-, \/}

trait CommonMarshallers extends MarshallingDirectives with Json4sSupport {

  implicit class DisjunctUnionFuture(f: Future[_]) {

    def mapRight[B]: Future[\/[String, B]] = f.mapTo[\/[String, B]]

  }

  implicit def duToResponseMarshaller[A : ToResponseMarshaller, B : ToResponseMarshaller]: ToResponseMarshaller[\/[A, B]] = {
    val lm = implicitly[ToResponseMarshaller[A]]
    val rm = implicitly[ToResponseMarshaller[B]]

    new ToResponseMarshaller[\/[A, B]] {
      override def apply(value: \/[A, B], ctx: ToResponseMarshallingContext): Unit = value match {
        case \/-(right) ⇒
          rm.apply(right, ctx)
        case -\/(left)  ⇒
          // TODO: BadRequest?!
          lm.apply(left, ctx.withResponseMapped(_.copy(status = StatusCodes.BadRequest)))
      }
    }
  }

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

  override implicit def json4sFormats: Formats = new DefaultFormats {
      override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    } + UUIDSerialiser

}
