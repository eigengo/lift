package com.eigengo.lift.spark

import akka.actor.{ Props, ActorLogging, Actor }
import com.eigengo.lift.spark.SparkServiceProtocol.{ Batch, Stream, SparkServiceProtocol }
import com.typesafe.config.Config

/**
 * Spark service metadata
 */
object SparkService {

  val name = "spark"

  /**
   * Spark service props
   * @param config
   * @return
   */
  def props(config: Config): Props = Props(new SparkService(config))
}

class SparkService(config: Config)
    extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case m: SparkServiceProtocol => m match {
      case Stream(d) =>
        println(s"stream $d")
      case Batch(d) =>
        println(s"batch $d")
    }
  }
}
