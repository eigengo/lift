package com.eigengo.lift.adapter

import java.util.UUID

import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.io.IO
import com.eigengo.lift.common.AdapterProtocol
import spray.can.Http
import spray.http._
import spray.routing.{Directives, RequestContext, Route}

import scala.util.Random

/**
 * Protocol for the ``RouteesActor``
 */
object AdapteesActor {
  import AdapterProtocol._

  case class Routee(ref: Reference, host: Host, port: Port, version: Version, side: Seq[Side])

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
  import AdapterProtocol._
  import com.eigengo.lift.adapter.AdapteesActor._
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(topic, self)

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

    val side = if (method == HttpMethods.GET || method == HttpMethods.OPTIONS || method == HttpMethods.OPTIONS) Query else Command

    routees.filter(r => r.version == versionPath.toString && r.side == side).randomElement.map { router =>
      uri.withHost(router.host).withPort(router.port).withPath(versionlessPath)
    }
  }

  def receive: Receive = {
    // register a new node
    case Register(address, api@RestApi(host, port, version, side)) ⇒
      log.info(s"Subscribed node at $address to $api")
      val ref = UUID.randomUUID().toString
      routees = routees :+ Routee(ref, host, port, version, side)
      sender() ! Registered(ref)
    // unregister a node
    case AdapterProtocol.Unregister(ref) =>
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
 * All other routes are proxied to the underlying node
 */
trait AdapteeRoute extends Directives {

  /** Route adds user identity header and proxies all requests to given host */
  def adapteesRoute(adaptees: ActorRef): Route = ctx => {
    adaptees ! ctx
  }

}
