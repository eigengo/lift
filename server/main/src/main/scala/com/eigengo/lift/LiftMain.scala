package com.eigengo.lift

import akka.actor._
import akka.io.IO
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.eigengo.lift.exercise._
import com.eigengo.lift.profile.UserProfileBoot
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.{HttpServiceActor, Route}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class LiftMain(routes: Seq[Route]) extends HttpServiceActor {
  override def receive: Receive = runRoute(routes.reduce(_ ~ _))
}

/**
 * CLI application for the exercise app
 */
object LiftMain extends App {
  val LiftActorSystem = "Lift"

  singleJvmStartup(Seq(2551 → "exercise", 2552 → "exercise", 2553 → "exercise", 2554 → "profile", 2556 → "profile"))

  def singleJvmStartup(ports: Seq[(Int, String)]): Unit = {
    ports.par.foreach { portAndRole ⇒
      val (port, role) = portAndRole
      import scala.collection.JavaConverters._
      // Override the configuration of the port
      val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=$role")
      val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=[$role]")
      val remotingConfig = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      val config = remotingConfig.withFallback(clusterShardingConfig).withFallback(clusterRoleConfig).withFallback(ConfigFactory.load("main.conf"))
      val firstSeedNodePort = (for {
        seedNode ← config.getStringList("akka.cluster.seed-nodes").asScala
        port ← ActorPath.fromString(seedNode).address.port
      } yield port).head

      // Create an Akka system
      implicit val system = ActorSystem(LiftActorSystem, config)
      Thread.sleep(2000)

      // Startup the journal
      startupSharedJournal(system, startStore = port == firstSeedNodePort, path = ActorPath.fromString(s"akka.tcp://$LiftActorSystem@127.0.0.1:$firstSeedNodePort/user/store"))

      // boot the microservices
      val routes = ListBuffer[(ExecutionContext ⇒ Route)]()
      if (role.contains("profile")) {
        val profile = UserProfileBoot.boot(system)
        profile.api.foreach(routes +=)
      }
      if (role.contains("exercise")) {
        val exercise = ExerciseBoot.bootCluster(system)
        exercise.api.foreach(routes +=)
      }

      startupHttpService(system, port, routes.map(_.apply(system.dispatcher)))
    }

    def startupHttpService(system: ActorSystem, port: Int, routes: Seq[Route]): Unit = {
      val restService = system.actorOf(Props(classOf[LiftMain], routes))
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = 10000 + port)
    }

    def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
      import akka.pattern.ask

import scala.concurrent.duration._
      
      // Start the shared journal one one node (don't crash this SPOF)
      // This will not be needed with a distributed journal
      if (startStore) system.actorOf(Props[SharedLeveldbStore], "store")

      // register the shared journal
      import system.dispatcher
      implicit val timeout = Timeout(15.seconds)
      val f = system.actorSelection(path) ? Identify(None)
      f.onSuccess {
        case ActorIdentity(_, Some(ref)) ⇒ SharedLeveldbJournal.setStore(ref, system)
        case _ =>
          system.log.error("Shared journal not started at {}", path)
          system.shutdown()
      }
      f.onFailure {
        case _ =>
          system.log.error("Lookup of shared journal at {} timed out", path)
          system.shutdown()
      }
    }
  }

}
