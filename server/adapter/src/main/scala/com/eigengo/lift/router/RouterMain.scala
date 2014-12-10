package org.eigengo.cqrsrest.router

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.io.IO
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats}
import spray.can.Http
import spray.routing.HttpServiceActor

/**
 * The actor wrapper around the routes
 *
 * @param routeesActor the actor that maintains the routees
 */
class RouterMainServiceActor(routeesActor: ActorRef) extends HttpServiceActor with RouteesRoute with ProxyRoute {
  implicit val ec = context.dispatcher
  override def receive: Receive = runRoute(routeesRoute(routeesActor) ~ proxyRoute(routeesActor))
}

/**
 * The main app for the router / load balancer.
 *
 * It accepts registrations from multiple nodes, each node specifying whether it is the read or write node, and
 * what API version it implements.
 */
object RouterMain extends App {
  import scala.concurrent.duration._

  implicit def json4sFormats: Formats = DefaultFormats
  implicit val timeout: Timeout = Timeout(1000 milliseconds)

  implicit private val system = ActorSystem()
  val routeesActor = system.actorOf(Props(new RouteesActor), "routees-actor")
  val service = system.actorOf(Props(new RouterMainServiceActor(routeesActor)), "router-service")
  val host = "0.0.0.0"
  val port = 8080

  IO(Http)(system) ! Http.Bind(service, interface = host, port = port)

}
