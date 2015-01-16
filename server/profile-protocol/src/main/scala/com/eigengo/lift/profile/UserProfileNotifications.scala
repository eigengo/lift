package com.eigengo.lift.profile

import akka.actor.{Actor, ActorRef}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.notification.NotificationProtocol.{PushMessagePayload, PushMessage, Devices}
import com.eigengo.lift.profile.UserProfileProtocol.UserGetDevices

import scala.concurrent.ExecutionContext

/**
 * Convenience trait that provides type that can send notification to all user devices
 */
trait UserProfileNotifications {
  this: Actor ⇒

  def newNotificationSender(userId: UserId, notification: ActorRef, userProfile: ActorRef): UserProfileNotificationsSender = {
    new UserProfileNotificationsSender(userId, notification, userProfile)(context.dispatcher)
  }
  
}

/**
 * Convenience sender
 * @param userId the user identity
 * @param notification the notification actor
 * @param userProfile the user profile actor
 * @param ec execution context
 */
private[profile] class UserProfileNotificationsSender(userId: UserId, notification: ActorRef, userProfile: ActorRef)(implicit ec: ExecutionContext) {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._
  private var devices: Devices = Devices.empty

  refreshDevices()

  private def refreshDevices(f: ⇒ Unit = {}): Unit = {
    (userProfile ? UserGetDevices(userId)).mapTo[Devices].onSuccess {
      case ds ⇒ devices = ds
    }
  }

  /**
   * Sends the notification to the last registered device, using the remembered list of devices
   * @param payload the payload to be sent
   */
  def !(payload: PushMessagePayload): Unit = notification ! PushMessage(devices.justLast, payload)

  /**
   * Sends the notification to the last registered device, refreshing the list of devices
   * @param payload the payload to be sent
   */
  def !?(payload: PushMessagePayload): Unit = refreshDevices(this ! payload)
  
}
