package com.eigengo.lift

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import com.eigengo.lift.exercise.ExerciseBoot
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.ProfileBoot

/**
 * This is the dockerified Microservice app main.
 */
object LiftServiceApp extends MicroserviceApp(MicroserviceProps("Lift")) {
  override def boot(implicit system: ActorSystem, cluster: Cluster): BootedNode = {
    val profile = ProfileBoot.boot(system)
    val notificaiton = NotificationBoot.boot(profile.userProfile)
    val exercise = ExerciseBoot.boot(notificaiton.notification)

    profile + notificaiton + exercise
  }
}
