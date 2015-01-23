package com.eigengo.lift

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import com.eigengo.lift.exercise.ExerciseBoot
import com.eigengo.lift.kafka.{KafkaBoot, KafkaProducerActor}
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.ProfileBoot
import com.typesafe.config.{Config, ConfigFactory}

/**
 * This is the dockerified Microservice app main.
 */
object LiftServiceApp extends MicroserviceApp(MicroserviceProps("Lift")) {
  override def boot(config: Config)(implicit system: ActorSystem, cluster: Cluster): BootedNode = {

    val kafka = KafkaBoot.boot(config)
    val profile = ProfileBoot.boot
    val notificaiton = NotificationBoot.boot
    val exercise = ExerciseBoot.boot(kafka.kafka, notificaiton.notification, profile.userProfile)

    profile + notificaiton + exercise
  }
}
