/*
package com.eigengo.lift.spark

import akka.actor.{ ActorRef, ActorSystem }
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.typesafe.config.Config

case class SparkBoot(spark: ActorRef) extends BootedNode

object SparkBoot {
  def boot(config: Config)(implicit system: ActorSystem): SparkBoot = {
    SparkBoot(system.actorOf(SparkService.props(config), SparkService.name))
  }
}*/
