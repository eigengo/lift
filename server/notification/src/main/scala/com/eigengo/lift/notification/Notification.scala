package com.eigengo.lift.notification

import akka.actor.{Props, Actor}

object Notification {
  val props = Props[Notification]
  val name = "notification"
}

class Notification extends Actor {
  override def receive: Receive = {
    case x ⇒ println(">>>>>>> " + x)
  }
}
