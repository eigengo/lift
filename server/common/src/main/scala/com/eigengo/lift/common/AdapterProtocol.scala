package com.eigengo.lift.common

import akka.actor.{ActorSystem, Address}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish

/**
 * Defines the router protocol
 */
object AdapterProtocol {
  /** The network interface for the host—"localhost" or "0.0.0.0" */
  type Host      = String
  /** The IP port 1024 — 64k */
  type Port      = Int
  /** The API version being registered; for example "1.0.1" */
  type Version   = String
  /** The reference to the registered side */
  type Reference = String

  /**
   * Sides are either the write for POST, PUT, DELETE or the read side for GET, HEAD, OPTIONS
   */
  sealed trait Side
  case object Query extends Side
  case object Command extends Side

  /**
   * The rest API details showing the bound houst, port, API version and Q/C "side"
   * @param host the host name
   * @param port the port number (accessible outside the host)
   * @param version the microservice version
   * @param side the "sides" of the CQRS devide the API serves
   */
  case class RestApi(host: Host, port: Port, version: Version, side: Seq[Side])

  /**
   * Registers the given ``host`` and ``port`` running a specified ``version`` of the query or write API
   *
   * @param address the AS address
   * @param restApi the REST API details
   */
  case class Register(address: Address, restApi: RestApi)

  /**
   * Request to unregister the node given explicit reference from the router
   * @param address the AS address
   */
  case class Unregister(address: Address)

  /**
   * Mediation topic
   */
  val topic = "Adapter.nodes"

  def register(address: Address, restApi: RestApi)(implicit system: ActorSystem): Unit = {
    import scala.concurrent.duration._
    val mediator = DistributedPubSubExtension(system).mediator

    import system.dispatcher
    system.scheduler.schedule(1.second, 10.seconds, mediator, Publish(topic, Register(address, restApi)))
  }

  def unregister(address: Address)(implicit system: ActorSystem): Unit = {
    val mediator = DistributedPubSubExtension(system).mediator

    mediator ! Publish(topic, Unregister(address))
  }

}
