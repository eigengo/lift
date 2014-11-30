package com.eigengo.lift

import akka.actor._
import akka.io.IO
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.eigengo.lift.exercise._
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.UserProfileBoot
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.{HttpServiceActor, Route}

class LiftMain(routes: Route*) extends HttpServiceActor {
  override def receive: Receive = runRoute(routes.reduce(_ ~ _))
}

/**
 * CLI application for the exercise app
 */
object LiftMain extends App {
  val LiftActorSystem = "Lift"

  singleJvmStartup(Seq(2551, 2552, 2553, 2554))

  def singleJvmStartup(ports: Seq[Int]): Unit = {
    ports.foreach { port ⇒
      import scala.collection.JavaConverters._
      // Override the configuration of the port
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load("main.conf"))
      val firstSeedNodePort = (for {
        seedNode ← config.getStringList("akka.cluster.seed-nodes").asScala
        port ← ActorPath.fromString(seedNode).address.port
      } yield port).head

      // Create an Akka system
      implicit val system = ActorSystem(LiftActorSystem, config)
      import system.dispatcher

      // Startup the journal
      startupSharedJournal(system, startStore = port == firstSeedNodePort, path = ActorPath.fromString(s"akka.tcp://$LiftActorSystem@127.0.0.1:$firstSeedNodePort/user/store"))

      // boot the microservices
      val profile = UserProfileBoot.boot(system)
      val notificaiton = NotificationBoot.bootResolved(profile.userProfile)
      val exercise = ExerciseBoot.bootResolved(notificaiton.notification)

      startupHttpService(system, port, exercise.route(system.dispatcher), profile.route(system.dispatcher))
    }

    def startupHttpService(system: ActorSystem, port: Int, routes: Route*): Unit = {
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
