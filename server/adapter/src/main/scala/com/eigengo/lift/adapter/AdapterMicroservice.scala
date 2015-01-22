package com.eigengo.lift.adapter

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import com.typesafe.config.Config
import spray.can.Http
import spray.routing._

import scala.concurrent.ExecutionContext

object AdapterBoot {
  def boot(implicit system: ActorSystem): BootedNode = {
    val adaptees = system.actorOf(AdapteesActor.props)
    val host = "0.0.0.0"
    val port = 8080

    IO(Http)(system) ! Http.Bind(adaptees, interface = host, port = port)

    BootedNode.empty
  }
}

object AdapterMicroservice extends MicroserviceApp(MicroserviceProps("Adapter")) {

  override def boot(config: Config)(implicit system: ActorSystem, cluster: Cluster): BootedNode = {
    AdapterBoot.boot
  }
}
