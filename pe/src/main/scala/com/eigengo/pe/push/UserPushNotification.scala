package com.eigengo.pe.push

import akka.actor.{Props, Actor}
import com.notnoop.apns.APNS

import scala.io.Source

/**
 * UserPushNotification protocol
 */
object UserPushNotification {
  val name: String = "user-push-notification"

  val props: Props = Props[UserPushNotification]

  /**
   * Sends default message to the client
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class DefaultMessage(message: String, badge: Option[Int], sound: Option[String])
}

/**
 * Pushes the notifications to the given user
 */
class UserPushNotification extends Actor {
  import com.eigengo.pe.push.UserPushNotification._

  private val userHomeIos = System.getProperty("user.home") + "/.ios"
  private val certificatePath = s"$userHomeIos/lift-push-development.p12"
  private val certificatePassword = Source.fromFile(s"$userHomeIos/lift-push-development.pwd").mkString
  private val service = APNS.newService.withCert(certificatePath, certificatePassword).withSandboxDestination.build

  override def receive: Receive = {
    case DefaultMessage(message, badge, sound) =>
      // lookup user device Id
      // "131af4f2 64f2c000 b5814833 90d01b87 f5cbd074 48bea21b 9b517640 97a5c74c"
      val deviceToken = "5ab84805 f8d0cc63 0a8990a8 4d480841 c3684003 6c122c8e 52a8dcfd 68a6f6f8"
      val payloadBuilder = APNS.newPayload.alertBody(message)
      badge.foreach(payloadBuilder.badge)
      sound.foreach(payloadBuilder.sound)
      service.push(deviceToken, payloadBuilder.build())
  }
}
