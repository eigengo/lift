package com.eigengo.lift.exercise

import akka.actor.{Actor, Props}
import com.eigengo.lift.kafka.KafkaProducer
import com.typesafe.config.Config

/**
 * Kafka producer actor companion for actor construction
 */
object KafkaProducerActor {

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
 * @param kafkaConfig kafka configuration
 */
class KafkaProducerActor(override val kafkaConfig: Config)
  extends Actor
  with KafkaProducer
  with JavaSerializationCodecs {

  override def receive: Receive = {
    case exercise: Exercise => {
      println(s"Producing $exercise")
      val produced = produce(exercise, "test")
      println(s"Produced $produced")
    }

    case "die" =>
      context stop self
  }
}
