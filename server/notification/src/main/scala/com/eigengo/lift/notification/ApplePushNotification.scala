package com.eigengo.lift.notification

import java.net.UnknownHostException

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Props, OneForOneStrategy, SupervisorStrategy, Actor}
import com.eigengo.lift.notification.ApplePushNotification.DefaultMessage
import com.notnoop.apns.APNS

import scala.io.Source

object ApplePushNotification {
  val props = Props[ApplePushNotification]

  /**
   * Sends default message to the client
   *
   * @param deviceToken the device token
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class DefaultMessage(deviceToken: String, message: String, badge: Option[Int], sound: Option[String])

}

class ApplePushNotification extends Actor {
  import scala.concurrent.duration._

  private val userHomeIos = System.getProperty("user.home") + "/.ios"
  private val certificatePath = s"$userHomeIos/lift-push-development.p12"
  private val certificatePassword = Source.fromFile(s"$userHomeIos/lift-push-development.pwd").mkString
  private val service = APNS.newService.withCert(certificatePath, certificatePassword).withSandboxDestination.build

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 10.seconds) {
    case x: UnknownHostException ⇒ Restart
  }

  override def receive: Receive = {
    case DefaultMessage(deviceToken, message, badge, sound) ⇒
      val payloadBuilder = APNS.newPayload.alertBody(message)
      badge.foreach(payloadBuilder.badge)
      sound.foreach(payloadBuilder.sound)
      service.push(deviceToken, payloadBuilder.build())
  }

}
