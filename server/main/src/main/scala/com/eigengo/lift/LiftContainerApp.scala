package com.eigengo.lift

import akka.actor.{ActorPath, ActorSystem}

/**
 * Docker containerised version of the (local) application. Single JVM and actor system running exercise, notification
 * and profile microservices. REST and Akka ports are fixed for each container.
 */
object LiftContainerApp extends App with LocalAppUtil {

  // In production, journal and snapshot stores are cassandra based - so setup is configuration/deployment based and so there is no work to do here!
  actorSystemStartUp(2552, 8080, "production.conf", { (ActorSystem, Boolean, ActorPath) => })

}
