package com.eigengo.lift.common

import akka.actor.{Actor, ReceiveTimeout}
import akka.contrib.pattern.ShardRegion.Passivate

import scala.concurrent.duration.Duration

trait AutoPassivation {
  this: Actor ⇒

  def passivationTimeout: Duration

  // the shard lives for the specified timeout seconds before passivating
  context.setReceiveTimeout(passivationTimeout)

  private val passivationReceive: Receive = {
    // passivation support
    case ReceiveTimeout ⇒
      context.parent ! Passivate(stopMessage = 'stop)
    case 'stop ⇒
      context.stop(self)
  }

  protected def withPassivation(receive: Receive): Receive = receive.orElse(passivationReceive)

}
