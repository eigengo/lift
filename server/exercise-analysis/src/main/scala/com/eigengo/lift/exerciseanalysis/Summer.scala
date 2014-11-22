package com.eigengo.lift.exerciseanalysis

import akka.actor.Actor
import org.apache.spark.streaming.receiver.ActorHelper

class Summer extends Actor with ActorHelper {

  override def receive: Receive = {
    case x ⇒ store(x)
  }

}
