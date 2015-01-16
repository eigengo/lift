package com.eigengo.lift.notification

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool

object Notification {
  val name = "notification"
  val props = Props(classOf[Notification]).withRouter(RoundRobinPool(nrOfInstances = 15))
}

class Notification extends Actor with ActorLogging {
  import com.eigengo.lift.notification.NotificationProtocol._
  private val apple = context.actorOf(ApplePushNotification.props)

  override def receive: Receive = {
    case PushMessage(devices, payload) ⇒
      devices.foreach {
        case IOSDevice(deviceToken) ⇒ apple ! ApplePushNotification.PushMessage(deviceToken, payload)
        case AndroidDevice() ⇒ log.debug(s"Not yet delivering Android push message $payload")
      }
  }
}
