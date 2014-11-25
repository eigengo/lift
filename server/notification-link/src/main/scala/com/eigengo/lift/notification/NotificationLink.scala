package com.eigengo.lift.notification

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterClient

object NotificationLink {
  private[notification] val notificationName = "notification"

  def notification(implicit system: ActorSystem): ActorRef = system.actorOf(ClusterClient.props(Set.empty))

}
