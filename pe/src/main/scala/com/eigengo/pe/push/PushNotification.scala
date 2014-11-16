package com.eigengo.pe.push

import akka.actor._

/**
 * UserPushNotification protocol
 */
object PushNotification {
  def lookup(implicit arf: ActorRefFactory): ActorSelection = arf.actorSelection(s"/user/$name")
  val name: String = "push-notification"
  val props: Props = Props[PushNotification]

  sealed trait NotificationAddress
  case class IOSNotificationAddress(deviceToken: String) extends NotificationAddress

  /**
   * Sends default message to the client
   *
   * @param address the recipient
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class DefaultMessage(address: NotificationAddress, message: String, badge: Option[Int], sound: Option[String])
}

/**
 * Pushes the notifications to the given user
 */
class PushNotification extends Actor {
  import com.eigengo.pe.push.PushNotification._

  val apple = context.actorOf(Props[ApplePushNotification])
  val android = context.actorOf(Props[AndroidPushNotification])

  // iPhone6: "5ab84805 f8d0cc63 0a8990a8 4d480841 c3684003 6c122c8e 52a8dcfd 68a6f6f8"

  override def receive: Receive = {
    case m@DefaultMessage(IOSNotificationAddress(_), _, _, _) => apple ! m
  }

}
