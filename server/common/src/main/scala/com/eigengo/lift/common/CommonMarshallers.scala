package com.eigengo.lift.common

import java.text.SimpleDateFormat
import java.util.UUID

import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{Serializer, CustomSerializer, DefaultFormats, Formats}
import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import spray.httpx.Json4sSupport
import spray.httpx.marshalling.{ToResponseMarshallingContext, ToResponseMarshaller}
import spray.routing.directives.MarshallingDirectives

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/-, \/}

object CommonMarshallers {

  private object ResponseOption {
    def apply[A](o: Option[A]): ResponseOption[A] = o match {
      case Some(v) ⇒ ResponseSome(v)
      case None    ⇒ ResponseNone
    }
  }
  sealed trait ResponseOption[+A]
  case object ResponseNone extends ResponseOption[Nothing]
  case class ResponseSome[A](value: A) extends ResponseOption[A]
  
}

trait CommonMarshallers extends MarshallingDirectives with Json4sSupport {
  import CommonMarshallers._

  implicit class DisjunctUnionFuture(f: Future[_]) {

    def mapRight[B]: Future[\/[String, B]] = f.mapTo[\/[String, B]]
    
    def mapNoneToEmpty[B](implicit ec: ExecutionContext): Future[ResponseOption[B]] = f.mapTo[Option[B]].map(ResponseOption.apply)

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

  implicit def responseOptionMarshaller[A : ToResponseMarshaller]: ToResponseMarshaller[ResponseOption[A]] = {
    val m = implicitly[ToResponseMarshaller[A]]

    new ToResponseMarshaller[ResponseOption[A]] {
      override def apply(value: ResponseOption[A], ctx: ToResponseMarshallingContext): Unit = value match {
        case ResponseNone    ⇒ ctx.marshalTo(HttpResponse(StatusCodes.OK, entity = HttpEntity(contentType = ContentTypes.`application/json`, string = "{}")))
        case ResponseSome(a) ⇒ m.apply(a, ctx)
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
