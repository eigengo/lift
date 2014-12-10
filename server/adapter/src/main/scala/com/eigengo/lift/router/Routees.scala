package org.eigengo.cqrsrest.router

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.io.IO
import akka.pattern.AskSupport
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats}
import spray.can.Http
import spray.http._
import spray.httpx.Json4sSupport
import spray.routing.{Directives, RequestContext, Route}

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
 * Protocol for the ``RouteesActor``
 */
object RouteesActor {
  import org.eigengo.cqrsrest.router.RouterProtocol._

  case class Routee(ref: Reference, host: Host, port: Port, version: Version, side: Side)

}

/**
 * This catch-all actor maintains the registered nodes and routes the received requests to the appropriate
 * node.
 *
 * Production implementation should check for version consistency—only allowing to register nodes of expected and
 * well-known version; it can also virtualise the versions, mapping—for example—``/1`` to ``/1.1.42``.
 */
class RouteesActor extends Actor {
  import org.eigengo.cqrsrest.router.RouteesActor._
  import org.eigengo.cqrsrest.router.RouterProtocol._

  private var routees: List[Routee] = List()

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

  private def findRoutee(uri: Uri, method: HttpMethod): Option[Uri] = {
    val path            = uri.path.tail
    val versionPath     = path.head
    val versionlessPath = path.tail

    println(versionlessPath.toString())

    val side = if (method == HttpMethods.GET || method == HttpMethods.OPTIONS || method == HttpMethods.OPTIONS) Query else Write

    routees.filter(r => r.version == versionPath.toString && r.side == side).randomElement.map { router =>
      uri.withHost(router.host).withPort(router.port).withPath(versionlessPath)
    }
  }

  def receive: Receive = {
    // register a new node
    case cmd@RouterProtocol.Register(_, _, _, _) =>
      val ref = UUID.randomUUID().toString
      routees = routees :+ Routee(ref, cmd.host, cmd.port, cmd.version, cmd.side)
      sender() ! Registered(ref)
    // unregister a node
    case RouterProtocol.Unregister(ref) =>
      routees = routees.filter(_.ref != ref)
      sender() ! Unregistered(ref)
    // proxy a request
    case ctx: RequestContext =>
      val request = ctx.request
      findRoutee(request.uri, request.method).fold
      {
        ctx.complete(HttpResponse(status = StatusCodes.BadGateway, entity = HttpEntity(s"No routee for path ${request.uri.path}")))
      }
      { updatedUri =>
        val updatedRequest = request.copy(uri = updatedUri, headers = stripHeaders(request.headers))
        IO(Http)(context.system) tell(updatedRequest, ctx.responder)
      }
  }
}

/**
 * This is the management endpoint that allows the nodes to register and de-register themselves.
 *
 * To register, POST the ``Register`` command, to unregister DELETE the ``Unregister`` command.
 */
trait RouteesRoute extends Directives with AskSupport with Json4sSupport {
  import org.eigengo.cqrsrest.router.RouterProtocol._

import scala.concurrent.duration._

  override implicit def json4sFormats: Formats = DefaultFormats + SideSerializer
  implicit val timeout: Timeout = Timeout(1000.milliseconds)

  def routeesRoute(routees: ActorRef)(implicit es: ExecutionContext): Route =
    path("register") {
      post {
        entity(as[Register]) { cmd =>
          complete((routees ? cmd).mapTo[Registered])
        }
      } ~
      delete {
        entity(as[Unregister]) { cmd =>
          complete((routees ? cmd).mapTo[Unregistered])
        }
      }
    }

}

/**
 * All other routes are proxied to the underlying node
 */
trait ProxyRoute extends Directives with AskSupport {

  /** Route adds user identity header and proxies all requests to given host */
  def proxyRoute(routees: ActorRef): Route = ctx => {
    routees ! ctx
  }

}
