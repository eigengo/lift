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
   * Response to ``Register`` carries the reference the router maintains for it. Use this ``ref`` to ``Unregister``
   * @param ref the generated reference
   */
  case class Registered(ref: Reference)

  /**
   * Request to unregister the node given explicit reference from the router
   * @param ref the reference obtained from ``Registered``
   */
  case class Unregister(ref: Reference)

  /**
   * Confirmation of unregistration
   * @param ref the reference obtained from ``Registered``
   */
  case class Unregistered(ref: Reference)

  /**
   * Mediation topic
   */
  val topic = "Adapter.nodes"

  def register(address: Address, restApi: RestApi)(implicit system: ActorSystem): Unit = {
    val mediator = DistributedPubSubExtension(system).mediator

    mediator ! Publish(topic, Register(address, restApi))
  }

}

/**
 * API for the router—call the ``apply`` function, specifying the router URL, side and version, and a function that
 * will be called upon successful registration. You should then bind to the given port and host.
 *
 * Notice that there is no two-phase registration: you are *expected* complete the bind.
object Router extends Json4sSupport {
  import org.eigengo.cqrsrest.router.RouterProtocol._
  import spray.client.pipelining._

  override implicit def json4sFormats: Formats = DefaultFormats + SideSerializer

  private def unregister(routerUrl: String, ref: Reference)(implicit arf: ActorRefFactory): Unit = {
    import arf.dispatcher
    val unregisterPipeline = sendReceive ~> unmarshal[Unregistered]
    unregisterPipeline(Delete(s"$routerUrl/register", Unregister(ref)))
  }

  def apply(routerUrl: String, side: Side, version: Version)(run: Run)(implicit arf: ActorRefFactory): Unit = {
    import arf.dispatcher
    val localhost = InetAddress.getLocalHost.getHostAddress
    val register = Register(localhost, 10000 + Random.nextInt(55535), version, side)
    val registerPipeline = sendReceive ~> unmarshal[Registered]
    registerPipeline(Post(s"$routerUrl/register", register)).onComplete {
      case Success(r) => run(RunParameters(register.host, register.port, () => unregister(routerUrl, r.ref)))
      case Failure(t) => throw t
    }
  }

}
*/
