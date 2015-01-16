package com.eigengo.lift.kafka

import akka.actor.{ActorLogging, Actor, Props}
import com.eigengo.lift.common.JavaSerializationCodecs
import com.typesafe.config.Config

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

  override val kafkaConfig = config.getConfig("kafka.producer")

  private val topic = kafkaConfig.getString("topic")

  private def logs[T](event: T) =
    log.info(s"""${self.path} producing "$event" to Kafka""")

  override def receive: Receive = {
    case event @ _ => {
      logs(event)
      val produced = produce(event, topic)
    }
  }

  override def postStop() = {
    close
    postStop
  }
}