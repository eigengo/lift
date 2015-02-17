package com.eigengo.lift

import akka.actor.{ActorPath, ActorSystem}
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps
import com.typesafe.config.ConfigFactory

/**
 * Docker containerised version of the (local) application. Single JVM and actor system running exercise, notification
 * and profile microservices. REST and Akka ports are fixed (by the provisioning system) for each container.
 */
object LiftMonolithApp extends App with LiftMonolith {

  // no journal startup code needed
  override def journalStartUp(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = ()

  // In production, Akka and REST port numbers are obtained from environment variables in production.conf
  lazy val config = {
    val rawConfig = ConfigFactory.load("production.conf")

    val microserviceProps = MicroserviceProps("Lift")
    val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=${microserviceProps.role}")
    val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=[${microserviceProps.role}]")
    // As the following configuration values are lists supplied by environment variables, we need to work a little harder here to ensure they are parsed correctly
    val seedingConfig = ConfigFactory.parseString(s"akka.cluster.seed-nodes=[${rawConfig.getString("akka.cluster.seed-nodes").split(",").map(_.trim).mkString("\"", "\",\"", "\"")}]")
    val journalConfig = ConfigFactory.parseString(s"cassandra-journal.contact-points=[${rawConfig.getString("cassandra-journal.contact-points").split(",").map(_.trim).mkString("\"", "\",\"", "\"")}]")
    val snapshotConfig = ConfigFactory.parseString(s"cassandra-snapshot-store.contact-points=[${rawConfig.getString("cassandra-snapshot-store.contact-points").split(",").map(_.trim).mkString("\"", "\",\"", "\"")}]")

    seedingConfig
      .withFallback(journalConfig)
      .withFallback(snapshotConfig)
      .withFallback(clusterShardingConfig)
      .withFallback(clusterRoleConfig)
      .withFallback(rawConfig)
  }

  val akkaPort = config.getInt("akka.remote.netty.tcp.port")
  val httpPort = config.getInt("akka.rest.port")
  actorSystemStartUp(akkaPort, httpPort)
}
