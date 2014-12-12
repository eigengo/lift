package com.eigengo.lift.common

import akka.actor.ActorSystem
import akka.event
import akka.event.{BusLogging, LogSource}
import scala.language.existentials

/**
 * Singleton logging class used to supply logging functionality to non-actor code
 */
object Logger {

  def apply[T : event.LogSource](source: T)(implicit system: ActorSystem) = {
    val (str, clazz) = LogSource(source, system)

    new BusLogging(system.eventStream, str, clazz) {
      def shutdown() = {
        system.shutdown()
      }
    }
  }

}
