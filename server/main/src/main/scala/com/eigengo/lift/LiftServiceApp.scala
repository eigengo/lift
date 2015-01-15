package com.eigengo.lift

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import com.eigengo.lift.exercise.{KafkaProducerActor, ExerciseBoot}
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.ProfileBoot
import com.typesafe.config.ConfigFactory

/**
 * This is the dockerified Microservice app main.
 */
object LiftServiceApp extends MicroserviceApp(MicroserviceProps("Lift")) {
  override def boot(implicit system: ActorSystem, cluster: Cluster): BootedNode = {

    //TODO: FIX
    val kafka = system.actorOf(KafkaProducerActor.props(ConfigFactory.parseString("")))
    val profile = ProfileBoot.boot(system)
    val notificaiton = NotificationBoot.boot
    val exercise = ExerciseBoot.boot(kafka, notificaiton.notification, profile.userProfile)

    profile + notificaiton + exercise
  }
}
