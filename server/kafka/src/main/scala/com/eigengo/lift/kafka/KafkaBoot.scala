package com.eigengo.lift.kafka

import akka.actor.{ActorSystem, ActorRef}
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.typesafe.config.Config

case class KafkaBoot(kafka: ActorRef) extends BootedNode

object KafkaBoot {
  def boot(config: Config)(implicit system: ActorSystem): KafkaBoot = {
    KafkaBoot(system.actorOf(KafkaProducerActor.props(config), KafkaProducerActor.name))
  }
}