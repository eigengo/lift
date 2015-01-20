package com.eigengo.lift.kafka

import com.eigengo.lift.common.{MessageEncoder, MessagePayload}
import MessagePayload.Payload
import com.eigengo.lift.kafka.PropertiesConfig._
import com.typesafe.config.Config
import kafka.javaapi.producer.Producer
import kafka.producer.{KeyedMessage, ProducerConfig}

import scalaz.{DisjunctionFunctions, \/}

/**
 * Producer of messages to Kafka
 *
 * If config uses producer.type = "async" setting, the producer batches messages for configured time or nr of messages
 *   There is no way to check if messages failed! There is a large refactor of the producer approach for Kafka 0.9
 *   Which might return Future instead of Unit
 *   Currently produce method will always return \/- for async case
 *
 * If config uses producer.type = "sync" the producer sends the message immediately and retries configurable nr of times
 *
 * For config options see http://kafka.apache.org/07/configuration.html
 */
trait KafkaProducer extends DisjunctionFunctions {

  /**
   * Kafka required configuration
   *
   * Required kafka.producer
   */
  def kafkaConfig: Config

  private lazy val producer =
    new Producer[String, Payload](new ProducerConfig(kafkaConfig.properties))

  //TODO: Implement retry for sync sending?
  //Similar to https://github.com/mighdoll/sparkle/blob/master/kafka/src/main/scala/nest/sparkle/loader/kafka/KafkaWriter.scala
  //(but working and without sleeping)
  //kafka producer internally has message.send.max.retries which should be enough
  private def send(message: KeyedMessage[String, Payload]): Throwable \/ Unit =
    fromTryCatchNonFatal(producer.send(message))

  /**
   * Encodes and produces a message to Kafka
   * @param item item to be sent to Kafka
   * @param topic topics
   * @tparam T type of the message to be produced
   * @return String with error message or Unit return value
   */
  def produce[T: MessageEncoder](item: T, topic: String): String \/ Unit = {
    implicitly[MessageEncoder[T]]
      .encode(item)
      .flatMap(m => send(new KeyedMessage(topic, m)).leftMap(_.toString))
  }

  /**
   * Closes the producer
   */
  def close() = producer.close
}
