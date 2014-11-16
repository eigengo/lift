package com.eigengo.lift.notification

object NotificationProtocol {

  sealed trait NotificationAddress
  case class IOSNotificationAddress(deviceToken: String) extends NotificationAddress

}
