package com.eigengo.lift

import akka.actor.{ActorPath, ActorSystem}
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps
import com.typesafe.config.ConfigFactory

/**
 * Docker containerised version of the (local) application. Single JVM and actor system running exercise, notification
 * and profile microservices. REST and Akka ports are fixed (by the provisioning system) for each container.
 */
object LiftContainerApp extends App with LocalAppUtil {

  // In production, Akka and REST port numbers are obtained from environment variables in production.conf
  val rawConfig = ConfigFactory.load("production.conf")
  val akkaPort = rawConfig.getInt("akka.remote.netty.tcp.port")
  val httpPort = rawConfig.getInt("akka.rest.port")

  val microserviceProps = MicroserviceProps("Lift")
  val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=${microserviceProps.role}")
  val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=[${microserviceProps.role}]")
  // As the following configuration values are lists supplied by environment variables, we need to work a little harder here to ensure they are parsed correctly
  val seedingConfig = ConfigFactory.parseString(s"akka.cluster.seed-nodes=[${rawConfig.getString("akka.cluster.seed-nodes").split(",").map(_.trim).mkString("\"", "\",\"", "\"")}]")
  val journalConfig = ConfigFactory.parseString(s"cassandra-journal.contact-points=[${rawConfig.getString("cassandra-journal.contact-points").split(",").map(_.trim).mkString("\"", "\",\"", "\"")}]")
  val snapshotConfig = ConfigFactory.parseString(s"cassandra-snapshot-store.contact-points=[${rawConfig.getString("cassandra-snapshot-store.contact-points").split(",").map(_.trim).mkString("\"", "\",\"", "\"")}]")

  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$akkaPort")
    .withFallback(seedingConfig)
    .withFallback(journalConfig)
    .withFallback(snapshotConfig)
    .withFallback(clusterShardingConfig)
    .withFallback(clusterRoleConfig)
    .withFallback(ConfigFactory.load("production.conf"))

  // In production, journal and snapshot stores are cassandra based - so setup is configuration/deployment based and so there is no work to do here!
  actorSystemStartUp(akkaPort, httpPort, config, { (ActorSystem, Boolean, ActorPath) => })

}
