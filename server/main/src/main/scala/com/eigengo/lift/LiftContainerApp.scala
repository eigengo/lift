package com.eigengo.lift

import akka.actor.{ActorPath, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
 * Docker containerised version of the (local) application. Single JVM and actor system running exercise, notification
 * and profile microservices. REST and Akka ports are fixed (by the provisioning system) for each container.
 */
object LiftContainerApp extends App with LocalAppUtil {

  // In production, Akka and REST port numbers are obtained from environment variables in production.conf
  val config = ConfigFactory.load("production.conf")
  val akkaPort = config.getInt("akka.remote.netty.tcp.port")
  val httpPort = config.getInt("akka.rest.port")

  // In production, journal and snapshot stores are cassandra based - so setup is configuration/deployment based and so there is no work to do here!
  actorSystemStartUp(akkaPort, httpPort, "production.conf", { (ActorSystem, Boolean, ActorPath) => })

}
