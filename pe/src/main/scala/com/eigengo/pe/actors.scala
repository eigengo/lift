package com.eigengo.pe

import akka.actor.{ActorSystem, ActorRefFactory, ActorRef, ActorContext}
import akka.contrib.pattern.ClusterSharding

object actors {

  // TODO: Logging!
  object shard {

    def lookup(arf: ActorRefFactory, shardName: String): ActorRef = arf match {
      case ctx: ActorContext ⇒ ClusterSharding(ctx.system).shardRegion(shardName)
      case sys: ActorSystem  ⇒ ClusterSharding(sys).shardRegion(shardName)
      case _                 ⇒ ActorRef.noSender
    }

  }

}
