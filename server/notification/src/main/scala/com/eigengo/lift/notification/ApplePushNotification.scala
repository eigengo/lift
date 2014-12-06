package com.eigengo.lift.notification

import java.net.UnknownHostException
import java.util

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.RoundRobinPool
import com.eigengo.lift.notification.ApplePushNotification.ScreenMessage
import com.notnoop.apns.{ApnsService, APNS}

import scala.io.Source
import scala.util.Try

object ApplePushNotification {
  val props = Props[ApplePushNotification].withRouter(RoundRobinPool(nrOfInstances = 10))

  /**
   * Sends default message to the client
   *
   * @param deviceToken the device token
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class ScreenMessage(deviceToken: Array[Byte], message: String, badge: Option[Int], sound: Option[String])

}

class ApplePushNotification extends Actor with ActorLogging {
  import scala.concurrent.duration._

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 10.seconds) {
    case x: UnknownHostException ⇒ Restart
  }

  override lazy val receive = Try {
    val userHomeIos = System.getProperty("user.home") + "/.ios"
    val certificatePath = s"$userHomeIos/lift-push-development.p12"
    val certificatePassword = Source.fromFile(s"$userHomeIos/lift-push-development.pwd").mkString
    val service = APNS.newService.withCert(certificatePath, certificatePassword).withSandboxDestination.build
    withCertificates(service)
  }.getOrElse(screenOnly)

  private def withCertificates(service: ApnsService): Receive = {
    case ScreenMessage(deviceToken, message, badge, sound) ⇒
      log.info(s"Screen message $message to ${util.Arrays.toString(deviceToken)}")
      val payloadBuilder = APNS.newPayload.alertBody(message)
      badge.foreach(payloadBuilder.badge)
      sound.foreach(payloadBuilder.sound)
      service.push(deviceToken, payloadBuilder.build().getBytes("UTF-8"))
  }

  private def screenOnly: Receive = {
    case ScreenMessage(deviceToken, message, _, _) ⇒
      log.info(s"*** Not delivering screen message $message to $deviceToken")
  }

}