package com.eigengo.lift.notification

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.ClusterReceptionistExtension
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.eigengo.lift.profile.UserProfileLink

case class NotificaitonBoot(notification: ActorRef) extends BootedNode

object NotificaitonBoot {

  def boot(system: ActorSystem): NotificaitonBoot = {
    val userProfile = UserProfileLink.userProfile(system)
    bootResolved(userProfile)(system)
  }

  def bootResolved(userProfile: ActorRef)(implicit system: ActorSystem): NotificaitonBoot = {
    val notification = system.actorOf(Notification.props(userProfile), NotificationLink.notificationName)
    ClusterReceptionistExtension(system).registerService(notification)
    NotificaitonBoot(notification)
  }

}