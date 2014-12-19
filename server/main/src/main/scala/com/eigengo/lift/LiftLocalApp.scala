package com.eigengo.lift

import akka.actor._
import akka.io.IO
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps
import com.eigengo.lift.exercise._
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.ProfileBoot
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.{HttpServiceActor, Route}

class LiftLocalApp(routes: Route*) extends HttpServiceActor {
  override def receive: Receive = runRoute(routes.reduce(_ ~ _))
}

/**
 * CLI application for the exercise app
 */
object LiftLocalApp extends App {
  val LiftActorSystem = "Lift"

  singleJvmStartup(Seq(2551, 2552, 2553, 2554))

  def singleJvmStartup(ports: Seq[Int]): Unit = {
    ports.par.foreach { port ⇒
      import scala.collection.JavaConverters._
      // Override the configuration of the port
      val microserviceProps = MicroserviceProps("Lift")
      val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=${microserviceProps.role}")
      val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=[${microserviceProps.role}]")
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(clusterShardingConfig).withFallback(clusterRoleConfig).withFallback(ConfigFactory.load("main.conf"))
      val firstSeedNodePort = (for {
        seedNode ← config.getStringList("akka.cluster.seed-nodes").asScala
        port ← ActorPath.fromString(seedNode).address.port
      } yield port).head

      // Create an Akka system
      implicit val system = ActorSystem(LiftActorSystem, config)

      // Startup the journal
      startupSharedJournal(system, startStore = port == firstSeedNodePort, path = ActorPath.fromString(s"akka.tcp://$LiftActorSystem@127.0.0.1:$firstSeedNodePort/user/store"))

      // boot the microservices
      val profile = ProfileBoot.boot(system)
      val notification = NotificationBoot.boot
      val exercise = ExerciseBoot.boot(notification.notification, profile.userProfile)

      startupHttpService(system, port, exercise.route(system.dispatcher), profile.route(system.dispatcher))
    }

    def startupHttpService(system: ActorSystem, port: Int, routes: Route*): Unit = {
      val restService = system.actorOf(Props(classOf[LiftLocalApp], routes))
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
