package com.eigengo.lift.common

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding

import scala.util.Try

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

}
