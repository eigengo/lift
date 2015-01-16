package com.eigengo.lift.notification

import java.net.UnknownHostException
import java.util

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.RoundRobinPool
import com.eigengo.lift.notification.ApplePushNotification.PushMessage
import com.eigengo.lift.notification.NotificationProtocol.{DataMessagePayload, ScreenMessagePayload, PushMessagePayload}
import com.notnoop.apns.{ApnsService, APNS}

import scala.io.Source
import scala.util.Try

object ApplePushNotification {
  val props = Props[ApplePushNotification].withRouter(RoundRobinPool(nrOfInstances = 10))

  /**
   * Sends default message to the client
   *
   * @param deviceToken the device token
   * @param payload the message
   */
  case class PushMessage(deviceToken: Array[Byte], payload: PushMessagePayload)

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
    case PushMessage(deviceToken, ScreenMessagePayload(message, badge, sound)) ⇒
      val payloadBuilder = APNS.newPayload.alertBody(message)
      badge.foreach(payloadBuilder.badge)
      sound.foreach(payloadBuilder.sound)
      service.push(deviceToken, payloadBuilder.build().getBytes("UTF-8"))
    case PushMessage(deviceToken, DataMessagePayload(message)) ⇒
      val payloadBuilder = APNS.newPayload.customField("data", message)
      service.push(deviceToken, payloadBuilder.build().getBytes("UTF-8"))
  }

  private def screenOnly: Receive = {
    case PushMessage(deviceToken, payload) ⇒
      log.debug(s"*** Not delivering screen message $payload to $deviceToken")
  }

}
