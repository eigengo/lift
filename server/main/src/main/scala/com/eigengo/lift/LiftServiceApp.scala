package com.eigengo.lift

import akka.actor.ActorSystem
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import com.eigengo.lift.exercise.ExerciseBoot
import com.eigengo.lift.notification.NotificationBoot
import com.eigengo.lift.profile.UserProfileBoot

object LiftServiceApp extends MicroserviceApp(MicroserviceProps("Lift")) {
  override def boot(implicit system: ActorSystem): BootedNode = {
    val profile = UserProfileBoot.boot(system)
    val notificaiton = NotificationBoot.boot(profile.userProfile)
    val exercise = ExerciseBoot.boot(notificaiton.notification)

    profile + notificaiton + exercise
  }
}
