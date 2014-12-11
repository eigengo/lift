package com.eigengo.lift.adapter

import akka.actor._
import akka.contrib.pattern.{ClusterInventory, ClusterInventoryGuardian}
import akka.io.{IO, Tcp}
import spray.can.Http
import spray.http._

import scala.util.Random

/**
 * Protocol for the ``RouteesActor``
 */
object AdapteesActor {
  import com.eigengo.lift.common.AdapterProtocol._

  case class Adaptee(address: Address, host: Host, port: Port, version: Version, side: Seq[Side])

  val props = Props[AdapteesActor]
}

/**
 * This catch-all actor maintains the registered nodes and routes the received requests to the appropriate
 * node.
 *
 * Production implementation should check for version consistency—only allowing to register nodes of expected and
 * well-known version; it can also virtualise the versions, mapping—for example—``/1`` to ``/1.1.42``.
 */
class AdapteesActor extends Actor with ActorLogging {
  import com.eigengo.lift.adapter.AdapteesActor._
  import com.eigengo.lift.common.AdapterProtocol._
  ClusterInventory(context.system).subscribe("", self)

  private var adaptees: List[Adaptee] = List()

  private implicit class RichList[A](l: List[A]) {
    def randomElement: Option[A] = l match {
      case Nil => None
      case nel => Some(nel(Random.nextInt(nel.size)))
    }
  }

  private def stripHeaders(headers: List[HttpHeader] = Nil) =
    headers filterNot { header =>
      (header is HttpHeaders.`Host`.lowercaseName) ||
      (header is HttpHeaders.`Content-Type`.lowercaseName) ||
      (header is HttpHeaders.`Content-Length`.lowercaseName)
    }

  private def findAdaptee(uri: Uri, method: HttpMethod): Option[Uri] = {
    val path            = uri.path.tail
    val versionPath     = path.head
    val versionlessPath = path.tail

    log.info(versionlessPath.toString())

    val side = if (method == HttpMethods.GET || method == HttpMethods.OPTIONS || method == HttpMethods.OPTIONS) Query else Command

    adaptees.filter(r => r.version == versionPath.toString && r.side.contains(side)).randomElement.map { router =>
      uri.withHost(router.host).withPort(router.port).withPath(versionlessPath)
    }
  }

  def receive: Receive = {
    case ClusterInventoryGuardian.KeyAdded(k, v) ⇒
      log.info(s"Would like to register $k and $v")

    /*
    case Register(address, api@RestApi(host, port, version, side)) ⇒
      if (!adaptees.exists(_.address == address)) {
        log.info(s"Registered node at $address to $api")
        adaptees = adaptees :+ Adaptee(address, host, port, version, side)
      } else {
        log.info(s"Already registered node at $address to $api")
      }
    // unregister a node
    case AdapterProtocol.Unregister(address) =>
      log.info(s"Unregistered node at $address")
      adaptees = adaptees.filter(_.address != address)
    */

    case Tcp.Connected(_, _) ⇒
      // by default we register ourselves as the handler for a new connection
      sender() ! Tcp.Register(self)

    // proxy a request
    case request: HttpRequest =>
      log.info(s"Handling ${request.uri} with adaptees $adaptees")
      findAdaptee(request.uri, request.method).fold
      {
        sender() ! HttpResponse(status = StatusCodes.BadGateway, entity = HttpEntity(s"No routee for path ${request.uri.path}"))
      }
      { updatedUri =>
        val updatedRequest = request.copy(uri = updatedUri, headers = stripHeaders(request.headers))
        IO(Http)(context.system) tell(updatedRequest, sender())
      }
    case x ⇒ println("Unhandled " + x)
  }
}
