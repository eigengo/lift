package com.eigengo.lift.profile

import akka.actor.{Actor, ActorRef}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.notification.NotificationProtocol.{MobileDestination, PushMessage, Devices}
import com.eigengo.lift.profile.UserProfileProtocol.UserGetDevices

import scala.concurrent.ExecutionContext

/**
 * Convenience trait that provides type that can send notification to all user devices
 */
trait UserProfileNotifications {
  this: Actor ⇒

  def allDevicesSender(userId: UserId, notification: ActorRef, userProfile: ActorRef): AllDevicesSender = {
    new AllDevicesSender(userId, notification, userProfile)(context.dispatcher)
  }
  
}

private[profile] class AllDevicesSender(userId: UserId, notification: ActorRef, userProfile: ActorRef)(implicit ec: ExecutionContext) {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._
  private var devices: Devices = Devices.empty

  (userProfile ? UserGetDevices(userId)).mapTo[Devices].onSuccess {
    case ds ⇒ devices = ds
  }

  def !(alert: String): Unit = {
    notification ! PushMessage(devices, alert, None, None, Seq(MobileDestination))
//    (userProfile ? UserGetDevices(userId)).mapTo[Devices].onSuccess {
//      case ds ⇒ notification ! PushMessage(ds, alert, None, None, Seq(MobileDestination))
//    }
  }
  
}