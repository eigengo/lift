package com.eigengo.lift

import akka.actor.{ActorPath, Props, ActorSystem}
import akka.io.IO
import com.eigengo.lift.exercise.ExerciseBoot
import com.eigengo.lift.kafka.KafkaBoot
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.ProfileBoot
import com.typesafe.config.Config
import spray.can.Http
import spray.routing._

trait LocalAppUtil {

  val LiftActorSystem = "Lift"

  def actorSystemStartUp(port: Int, restPort: Int, config: Config, journalStartUp: (ActorSystem, Boolean, ActorPath) => Unit): Unit = {
    import scala.collection.JavaConverters._
    // Override the configuration of the port
    val firstSeedNodePort = (for {
      seedNode ← config.getStringList("akka.cluster.seed-nodes").asScala
      port ← ActorPath.fromString(seedNode).address.port
    } yield port).head

    // Create an Akka system
    implicit val system = ActorSystem(LiftActorSystem, config)

    // Startup the journal - typically this is only used when running locally with a levelDB journal
    journalStartUp(system, port == firstSeedNodePort, ActorPath.fromString(s"akka.tcp://$LiftActorSystem@127.0.0.1:$firstSeedNodePort/user/store"))

    // boot the microservices
    val kafka = KafkaBoot.boot(config)
    val profile = ProfileBoot.boot
    val notification = NotificationBoot.boot
    val exercise = ExerciseBoot.boot(kafka.kafka, notification.notification, profile.userProfile)

    startupHttpService(system, restPort, exercise.route(system.dispatcher), profile.route(system.dispatcher))
  }

  def startupHttpService(system: ActorSystem, port: Int, routes: Route*): Unit = {
    val restService = system.actorOf(Props(classOf[LiftLocalApp], routes))
    IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = port)
  }

}
