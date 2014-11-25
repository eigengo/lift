package com.eigengo.lift.common

import akka.actor._
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import spray.routing.Route

import scala.concurrent.ExecutionContext

trait BootedNode {
  def api: Option[ExecutionContext ⇒ Route] = None
}

abstract class MicroserviceApp(port: Int)(f: ActorSystem ⇒ BootedNode) extends App {
  val LiftActorSystem = "Lift"

  // HACK: Wait for Cassandra startup.
  // TODO: Replace with etcd
  Thread.sleep(10000)

  def startup(): BootedNode = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem(LiftActorSystem, config)
    val cluster = Cluster(system)
    cluster.join(Address("tcp", "Lift"))
    f(system)
  }

  startup()

  // TODO: Shutdown
  // TODO: Logging
  println(">>> Running")
}
