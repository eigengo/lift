package com.eigengo.lift.common

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import scala.util.{Failure, Success, Try}
import scalaz.\/
import com.eigengo.lift.common.MessagePayload._

trait JavaSerializationCodecs {

  implicit def messageDecoder[A]: MessageDecoder[A] =
    new MessageDecoder[A] {
      override def decode(data: Payload): String \/ A = {
        val ois = new ObjectInputStream(new ByteArrayInputStream(data))
        val res = Try(ois.readObject().asInstanceOf[A]) match {
          case Success(a) ⇒ \/.right(a)
          case Failure(ex) ⇒ \/.left(ex.getMessage)
        }
        ois.close()

        res
      }
    }

  implicit def messageEncoder[A]: MessageEncoder[A] =
    new MessageEncoder[A] {
      override def encode(value: A): String \/ Payload = {
        val bos = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(bos)
        Try { oos.writeObject(value); oos.close() } match {
          case Success(_) ⇒ \/.right(bos.toByteArray)
          case Failure(ex) ⇒ \/.left(ex.getMessage)
        }

      }
    }
}
