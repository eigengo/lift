package com.eigengo.pe

import akka.actor.{ActorRefFactory, ActorSelection}

/**
 * Groups the actors so that their names do not diverge; it also contains convenience functions for actor lookups
 */
object actors {

  /**
   * The UserPushNotification definition.
   */
  object pushNotification {
    /** The actor name */
    val name = "push-notification"
    /** The lookup function */
    def apply(implicit arf: ActorRefFactory): ActorSelection = arf.actorSelection(s"/user/$name")
  }

}
