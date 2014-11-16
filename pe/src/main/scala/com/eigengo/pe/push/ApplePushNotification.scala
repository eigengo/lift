package com.eigengo.pe.push

import java.net.UnknownHostException

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, SupervisorStrategy, Actor}
import com.notnoop.apns.APNS

import scala.io.Source

class ApplePushNotification extends Actor {
  import PushNotification._
  import scala.concurrent.duration._

  private val userHomeIos = System.getProperty("user.home") + "/.ios"
  private val certificatePath = s"$userHomeIos/lift-push-development.p12"
  private val certificatePassword = Source.fromFile(s"$userHomeIos/lift-push-development.pwd").mkString
  private val service = APNS.newService.withCert(certificatePath, certificatePassword).withSandboxDestination.build

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 10.seconds) {
    case x: UnknownHostException ⇒ Restart
  }

  override def receive: Receive = {
    case DefaultMessage(IOSNotificationAddress(deviceToken), message, badge, sound) ⇒
      val payloadBuilder = APNS.newPayload.alertBody(message)
      badge.foreach(payloadBuilder.badge)
      sound.foreach(payloadBuilder.sound)
      service.push(deviceToken, payloadBuilder.build())
  }

}
