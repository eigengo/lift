package com.eigengo.lift.notification

import akka.actor.ActorRefFactory
import com.eigengo.lift.common.Actors

object NotificationProtocol {

  sealed trait NotificationAddress
  case class IOSNotificationAddress(deviceToken: String) extends NotificationAddress

  val shardName = "user-notification"
  def lookup(implicit arf: ActorRefFactory) = Actors.shard.lookup(arf, shardName)

}
