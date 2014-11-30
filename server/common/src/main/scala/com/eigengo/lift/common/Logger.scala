package com.eigengo.lift.common

import akka.actor.ActorSystem
import akka.event
import akka.event.{BusLogging, LogSource}
import scala.language.existentials

/**
 * Singleton logging class used to supply logging functionality to non-actor code
 */
object Logger {

  private var logSystem = Option.empty[ActorSystem]

  def apply[T : event.LogSource](source: T) = {
    logSystem = Some(logSystem.getOrElse(ActorSystem("LoggingActorSystem")))

    val (str, clazz) = LogSource(source, logSystem.get)

    new BusLogging(logSystem.get.eventStream, str, clazz) {
      def shutdown() = {
        logSystem.map(_.shutdown())
        logSystem = None
      }
    }
  }

}
