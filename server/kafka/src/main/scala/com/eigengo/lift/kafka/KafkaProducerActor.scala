package com.eigengo.lift.kafka

import akka.actor.{ActorLogging, Actor, Props}
import com.eigengo.lift.common.JavaSerializationCodecs
import com.typesafe.config.Config
import scala.collection.JavaConverters.asScalaSetConverter

import scalaz.\/
import com.eigengo.lift.kafka.PropertiesConfig._
import scala.language.implicitConversions

/**
 * Kafka producer actor companion for actor construction
 */
object KafkaProducerActor {

  val name = "kafka-producer"

  /**
   * KafkaProducerActor props
   * @param kafkaConfig
   * @return
   */
  def props(kafkaConfig: Config): Props = Props(new KafkaProducerActor(kafkaConfig))
}

/**
 * Kafka producer actor
 * Provides service to produce messages to Kafka
 * @param config kafka configuration
 */
class KafkaProducerActor(config: Config)
  extends Actor
  with ActorLogging
  with KafkaProducer
  with JavaSerializationCodecs {

  override def kafkaConfig = config.getConfig("kafka.producer")

  private val topic = kafkaConfig.getString("topic")

  private def logs[T](event: T, result: String \/ Unit) =
    log.info(s"""${self.path} producing "$event" to Kafka with result "$result"""")

  override def receive: Receive = {
    case event =>
      val produced = produce(event, topic)
      logs(event, produced)
  }

  override def postStop() = {
    close
    super.postStop
  }
}