package com.eigengo.lift

import collection.JavaConversions._
import akka.actor._
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps
import com.typesafe.config.ConfigFactory
import spray.routing.{HttpServiceActor, Route}

import scala.concurrent.Await

/**
 * CLI application for the exercise app
 */
object LiftLocalMonolithApp extends App with LiftMonolith {

  lazy val config = {
    val microserviceProps = MicroserviceProps("Lift")
    val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=${microserviceProps.role}")
    val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=[${microserviceProps.role}]")

    clusterShardingConfig
      .withFallback(clusterRoleConfig)
      .withFallback(ConfigFactory.load("main.conf"))
  }

  override def journalStartUp(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    import akka.pattern.ask
    import scala.concurrent.duration._

    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore) {
      system.actorOf(Props[SharedLeveldbStore], "store")
    }

    // register the shared journal
    implicit val timeout = Timeout(15.seconds)
    val f = system.actorSelection(path) ? Identify(None)
    Await.result(f, 15.seconds) match {
      case ActorIdentity(_, Some(ref)) ⇒ SharedLeveldbJournal.setStore(ref, system)
      case x ⇒
        system.log.error("Shared journal not started at {}", path)
        system.shutdown()
    }
  }

  val ports = config.getIntList("akka.cluster.jvm-ports")
  ports.foreach(port ⇒ actorSystemStartUp(port, 10000 + port))
}
