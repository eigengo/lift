package com.eigengo.lift.kafka

import PropertiesConfig._
import com.typesafe.config.Config
import kafka.common.FailedToSendMessageException
import kafka.javaapi.producer.Producer
import kafka.producer.{KeyedMessage, ProducerConfig}
import kafka.serializer.Encoder

import scala.annotation.tailrec
import scala.util.control.Exception._
import scala.util.{Failure, Success, Try}

trait KafkaProducer {

  val kafkaConfig: Config

  private val config = kafkaConfig.getConfig("kafka.producer")

  private lazy val SendMaxRetries = config.getInt("send-max-retries")

  private lazy val producer = {
    new Producer[String, Array[Byte]](new ProducerConfig(config.properties))
  }

  @tailrec
  private def send(message: KeyedMessage[String, Array[Byte]], retries: Int): Try[Unit] = {
    val result = nonFatalCatch withTry { producer.send(message) }

    //TODO: LOGS???
    result match {
      case Success(_)   => result
      case Failure(err) =>
        err match {
          case e: FailedToSendMessageException =>
            println(e.getMessage)
            retries match {
              case _ if retries <= SendMaxRetries =>
                send(message, retries - 1)
              case _                              =>
                result
            }
          case e :Throwable =>
            throw e
            result
        }
    }
  }

  def produce[T: Encoder](item: T, topic: String): Try[Unit] =
    send(new KeyedMessage[String, Array[Byte]](topic, implicitly[Encoder[T]].toBytes(item)), SendMaxRetries)
}
