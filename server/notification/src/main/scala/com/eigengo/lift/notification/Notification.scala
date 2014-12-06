package com.eigengo.lift.notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool
import com.eigengo.lift.notification.NotificationProtocol.{WatchDestination, MobileDestination, PushMessage}
import com.eigengo.lift.profile.UserProfileProtocol.{AndroidUserDevice, IOSUserDevice, UserDevices, UserGetDevices}

object Notification {
  val name = "notification"
  def props(userProfile: ActorRef) = Props(classOf[Notification], userProfile).withRouter(RoundRobinPool(nrOfInstances = 15))
}

class Notification(userProfile: ActorRef) extends Actor with ActorLogging {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._
  import context.dispatcher
  private val apple = context.actorOf(ApplePushNotification.props)

  override def receive: Receive = {
    case PushMessage(userId, message, badge, sound, destinations) ⇒
      (userProfile ? UserGetDevices(userId)).mapTo[UserDevices].onSuccess {
        case ud ⇒ ud.foreach {
          case IOSUserDevice(deviceToken) ⇒
            destinations.foreach {
              case MobileDestination ⇒ apple ! ApplePushNotification.ScreenMessage(deviceToken, message, badge, sound)
              case WatchDestination ⇒ // noop for now
            }

          case AndroidUserDevice() ⇒
            log.info(s"Not yet delivering Android push message $message")
        }
      }
  }
}
