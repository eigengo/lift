package com.eigengo.lift.common

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.contrib.pattern.{ClusterClient, ClusterSharding}

import scala.util.Try

object MicroserviceLink {
  
  private class SendToActor(path: String, client: ActorRef) extends Actor {
    override def receive: Receive = {
      case x ⇒ client.tell(ClusterClient.Send(s"/user/$path", x, localAffinity = true), sender())
    }
  }
  
}

trait MicroserviceLink {

  def clusterShardedLink(typeName: String)(implicit system: ActorSystem): ActorRef = {
    val allowedAttempts = 10
    
    def resolve(n: Int): ActorRef = {
      if (n > allowedAttempts) ActorRef.noSender
      else Try(ClusterSharding(system).shardRegion(typeName)).getOrElse {
        Thread.sleep((n + 1) * 1000) 
        resolve(n + 1)
      }
    }
    
    resolve(0)
  }

  def clusterClientLink(path: String)(implicit system: ActorSystem): ActorRef = {
    import MicroserviceLink._
    val client = system.actorOf(ClusterClient.props(Set.empty))
    system.actorOf(Props(classOf[SendToActor], path, client))
  }

}
