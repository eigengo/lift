package com.eigengo.lift.common

import akka.actor.{ActorLogging, Actor, ReceiveTimeout}

trait AutoPassivation extends ActorLogging {
  this: Actor ⇒

  import akka.contrib.pattern.ShardRegion.Passivate

  private val passivationReceive: Receive = {
    // passivation support
    case ReceiveTimeout ⇒
      log.debug("ReceiveTimeout: passivating.")
      context.parent ! Passivate(stopMessage = 'stop)
    case 'stop ⇒
      log.debug("'stop: bye-bye, cruel world, see you after recovery.")
      context.stop(self)
  }

  protected def withPassivation(receive: Receive): Receive = receive.orElse(passivationReceive)

}
