package com.eigengo.lift.common

import java.util.UUID

import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{CustomSerializer, DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing.directives.MarshallingDirectives

trait CommonMarshallers extends MarshallingDirectives with Json4sSupport {

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

}
