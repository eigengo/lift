package com.eigengo.lift.notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool
import com.eigengo.lift.notification.NotificationProtocol.PushMessage
import com.eigengo.lift.profile.UserProfileProtocol.{AndroidUserDevice, IOSUserDevice, UserDevices, UserGetDevices}

object Notification {
  def props(userProfile: ActorRef) = Props(classOf[Notification], userProfile).withRouter(RoundRobinPool(nrOfInstances = 15))
  val name = "notification"
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
          case IOSUserDevice(deviceToken) ⇒ apple ! ApplePushNotification.DefaultMessage(deviceToken, message, badge, sound)
          case AndroidUserDevice() ⇒
        }
      }
  }
}
