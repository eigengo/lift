package com.eigengo.lift.kafka

import akka.actor.{Props, Actor}
import com.typesafe.config.ConfigUtil
import kafka.producer.ProducerConfig
import kafka.javaapi.producer.Producer

object KafkaProducerActor {
  def props = Props[KafkaProducerActor]
}

class KafkaProducerActor extends Actor {
  def producer() = {
    val properties = ConfigUtil.properties(writerConfig.getConfig("producer"))
    new Producer[String, Array[Byte]](new ProducerConfig(properties))
  }


  override def receive: Receive = {
    case a@_ => print(a)
  }
}
