package com.eigengo.lift.exercise

import akka.actor.{Actor, Props}
import com.eigengo.lift.kafka.KafkaProducer
import com.typesafe.config.Config
import kafka.serializer.Encoder

object KafkaProducerActor {
  def props(kafkaConfig: Config) = Props(new KafkaProducerActor(kafkaConfig))
}

class KafkaProducerActor(override val kafkaConfig: Config) extends Actor with KafkaProducer {

  implicit val encoder: Encoder[Exercise] = new Encoder[Exercise] {
    override def toBytes(t: Exercise): Array[Byte] = t.toString.getBytes("UTF8")
  }

  override def receive: Receive = {
    case exercise: Exercise => {
      println(s"Producing $exercise")
      produce(exercise, "test")
    }

    case "die" =>
      context stop self
  }
}
