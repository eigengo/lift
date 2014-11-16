package com.eigengo.lift.notification

import akka.actor.{ActorRef, Props, Actor}

object Notification {
  def props(userProfile: ActorRef) = Props(classOf[Notification], userProfile)
  val name = "notification"
}

class Notification extends Actor {
  override def receive: Receive = {
    case x ⇒ println(">>>>>>> " + x)
  }
}
