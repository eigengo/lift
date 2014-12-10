package com.eigengo.lift.adapter

import akka.actor.{ActorRef, ActorSystem}
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import spray.routing._

import scala.concurrent.ExecutionContext

case class AdapterBoot(adaptees: ActorRef) extends BootedNode {
  import com.eigengo.lift.adapter.AdapterBoot._

  def route(ec: ExecutionContext): Route = { _ ⇒ adapteesRoute(adaptees) }

  override def api: Option[(ExecutionContext) ⇒ Route] = Some(route)

}

object AdapterBoot extends AdapteeRoute {
  def boot(implicit system: ActorSystem): AdapterBoot = {
    val adaptees = system.actorOf(AdapteesActor.props)
    AdapterBoot(adaptees)
  }
}

object AdapterMicroservice extends MicroserviceApp(MicroserviceProps("Adapter")) {

  override def boot(implicit system: ActorSystem): BootedNode = {
    val adaptees = system.actorOf(AdapteesActor.props)
    AdapterBoot(adaptees)
  }
}
