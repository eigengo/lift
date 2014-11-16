package com.eigengo.lift.notification

import com.eigengo.lift.common.UserId

object NotificationProtocol {

  sealed trait Destination
  case object MobileDestination extends Destination
  case object WatchDestination extends Destination
  
  /**
   * Sends default message to the client
   *
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class PushMessage(user: UserId, message: String, badge: Option[Int], sound: Option[String], destinations: Seq[Destination])

}
