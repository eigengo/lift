package com.eigengo.lift.kafka

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigUtil}
import kafka.common.FailedToSendMessageException
import kafka.javaapi.producer.Producer
import kafka.producer.{KeyedMessage, ProducerConfig}
import kafka.serializer.Encoder
import scala.annotation.tailrec
import scala.collection.JavaConverters.{asJavaIterableConverter, asScalaBufferConverter, asScalaSetConverter}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import scala.util.control.Exception._
import scala.util.control.NonFatal

import scala.util.{Failure, Success, Try}

trait KafkaProducer {

  val kafkaConfig: Config

  private lazy val SendMaxRetries = kafkaConfig.getInt("send-max-retries")

  private lazy val producer = {
    new Producer[String, Array[Byte]](new ProducerConfig(properties(kafkaConfig.getConfig("producer"))))
  }

  private def properties(config: Config): java.util.Properties = {
    val entries =
    config.entrySet.asScala.map { entry =>
        val key = entry.getKey
        val value = config.getString(key)
        (key, value)
      }
    val properties = new java.util.Properties()
    entries.foreach { case (key, value) => properties.put(key, value) }
    properties
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
            retries match {
              case _ if retries <= SendMaxRetries =>
                send(message, retries - 1)
              case _                              =>
                result
            }
          case _ => result
        }
    }
  }

  def produce[T: Encoder](item: T, topic: String, config: Config): Try[Unit] =
    send(new KeyedMessage[String, Array[Byte]](topic, implicitly[Encoder[T]].toBytes(item)), SendMaxRetries)
}
