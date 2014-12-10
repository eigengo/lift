package org.eigengo.cqrsrest.router

import java.net.InetAddress

import akka.actor.ActorRefFactory
import org.json4s.JsonAST.JString
import org.json4s.{CustomSerializer, DefaultFormats, Formats}
import spray.httpx.Json4sSupport

import scala.util.{Failure, Random, Success}

/**
 * Defines the router protocol
 */
object RouterProtocol {
  /** The network interface for the host—"localhost" or "0.0.0.0" */
  type Host      = String
  /** The IP port 1024 — 64k */
  type Port      = Int
  /** The API version being registered; for example "1.0.1" */
  type Version   = String
  /** The reference to the registered side */
  type Reference = String

  /** A function that runs when the side registers with the router */
  type Run       = RunParameters => Unit

  /**
   * Run parameters include the generated host and port, and the unregister function. The side should
   * bind to the given host and port, and it may call ``unregister`` to detach from the router.
   *
   * Once detached, it should exit.
   *
   * @param host the host (IP or name)
   * @param port the port
   * @param unregister the unregister function
   */
  case class RunParameters(host: Host, port: Port, unregister: () => Unit)

  /**
   * Sides are either the write for POST, PUT, DELETE or the read side for GET, HEAD, OPTIONS
   */
  sealed trait Side
  case object Query extends Side
  case object Write extends Side

  /**
   * Registers the given ``host`` and ``port`` running a specified ``version`` of the query or write API
   *
   * @param host the host (this can be localhost, but should really be a resolvable name or IP)
   * @param port the port the API is bound to
   * @param version the API version
   * @param side the side
   */
  case class Register(host: Host, port: Port, version: Version, side: Side)

  /**
   * Response to ``Register`` carries the reference the router maintains for it. Use this ``ref`` to ``Unregister``
   * @param ref the generated reference
   */
  case class Registered(ref: Reference)

  case class Unregister(ref: Reference)
  case class Unregistered(ref: Reference)

  object SideSerializer extends CustomSerializer[Side](side => (
    {
      case JString("query") => Query
      case JString("write") => Write
    },
    {
      case Query => JString("query")
      case Write => JString("write")
    }
    )
  )
}

/**
 * API for the router—call the ``apply`` function, specifying the router URL, side and version, and a function that
 * will be called upon successful registration. You should then bind to the given port and host.
 *
 * Notice that there is no two-phase registration: you are *expected* complete the bind.
 */
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
