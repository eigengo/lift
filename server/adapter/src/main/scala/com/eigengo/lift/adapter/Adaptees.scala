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

  object Adaptee {
    val Address = "http://(.*):(\\d+)(.*)\\?version=(.*)&side=(.*)".r

    def unapply(key: String, s: String): Option[Adaptee] = s match {
      case Address(host, port, path, version, side) ⇒
        Some(Adaptee(key, host, port.toInt, version, Command :: Query :: Nil))
      case _ ⇒ None
    }
  }
  case class Adaptee(key: String, host: Host, port: Port, version: Version, side: Seq[Side])

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
  ClusterInventory(context.system).subscribe("api", self, true)

  private var adaptees: List[Adaptee] = List.empty

  private implicit class RichSet[A](l: List[A]) {
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
      Adaptee.unapply(k, v).foreach { adaptee ⇒
        if (!adaptees.contains(adaptee)) {
          adaptees = adaptee :: adaptees
          log.info(s"Registered endpoint. Now with $adaptees.")
        }
      }

    case ClusterInventoryGuardian.KeyRemoved(key) ⇒
      adaptees = adaptees.dropWhile(_.key == key)
      log.info(s"Dropped endpoint. Now with $adaptees.")

    case Tcp.Connected(_, _) ⇒
      // by default we register ourselves as the handler for a new connection
      sender() ! Tcp.Register(self)

    // proxy a request
    case request: HttpRequest =>
      findAdaptee(request.uri, request.method).fold
      {
        sender() ! HttpResponse(status = StatusCodes.BadGateway, entity = HttpEntity(s"No routee for path ${request.uri.path}"))
      }
      { updatedUri =>
        log.info(s"Sending ${request.uri} to $updatedUri")
        val updatedRequest = request.copy(uri = updatedUri, headers = stripHeaders(request.headers))
        IO(Http)(context.system) tell(updatedRequest, sender())
      }
    case x ⇒ println("Unhandled " + x)
  }
}
