package com.eigengo.lift.kafka

import akka.actor.{Props, Actor}
import com.typesafe.config.{Config, ConfigUtil}
import kafka.producer.ProducerConfig
import kafka.javaapi.producer.Producer

object KafkaProducerActor {
  def props = Props[KafkaProducerActor]
}

class KafkaProducerActor(override val kafkaConfig: Config) extends Actor with KafkaProducer {

  override def receive: Receive = {
    case a@_ => print(a)
  }

}
