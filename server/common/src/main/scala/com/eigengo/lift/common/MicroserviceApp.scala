package com.eigengo.lift.common

import akka.actor._
import akka.cluster.Cluster
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.{HttpServiceActor, Route}

import scala.concurrent.ExecutionContext

object MicroserviceApp {

  class Api(routes: Route*) extends HttpServiceActor {
    override def receive: Receive = runRoute(routes.reduce(_ ~ _))
  }

  trait BootedNode {
    def api: Option[ExecutionContext ⇒ Route] = None
  }

}

abstract class MicroserviceApp(port: Int)(f: ActorSystem ⇒ BootedNode) extends App {
  import MicroserviceApp._
  private val name = "Lift"

  // HACK: Wait for Cassandra startup.
  // TODO: Replace with etcd
  Thread.sleep(10000)

  def startup(): Unit = {
    def startupApi(system: ActorSystem)(api: ExecutionContext ⇒ Route): Unit = {
      val restService = system.actorOf(Props(classOf[Api], api(system.dispatcher)))
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = 8080)
    }

    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem(name, config)
    val cluster = Cluster(system)
    cluster.joinSeedNodes(Address("akka.tcp", name, "localhost", 2551) :: Address("akka.tcp", name, "localhost", 2552) :: Nil)

    // now we've booted
    val bootedNode = f(system)
    bootedNode.api.foreach(startupApi(system))
  }


  startup()

  // TODO: Shutdown
  // TODO: Logging
  println(">>> Running")
}
