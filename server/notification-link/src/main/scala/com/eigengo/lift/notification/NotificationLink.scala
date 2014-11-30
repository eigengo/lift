package com.eigengo.lift.notification

import akka.actor.{ActorRef, ActorSystem}
import com.eigengo.lift.common.MicroserviceLink

object NotificationLink extends MicroserviceLink {
  private[notification] val notificationName = "notification"

  def notification(implicit system: ActorSystem): ActorRef = clusterClientLink(notificationName)

}
