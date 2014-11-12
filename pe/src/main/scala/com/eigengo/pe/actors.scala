package com.eigengo.pe

import akka.actor._
import akka.contrib.pattern.ClusterSharding

object actors {

  object local {
    def lookup(arf: ActorRefFactory, name: String): ActorSelection = arf match {
      case ctx: ActorContext ⇒ ctx.system.actorSelection(s"/user/$name")
      case sys: ActorSystem  ⇒ sys.actorSelection(s"/user/$name")
    }
  }

  // TODO: Logging!
  object shard {

    def lookup(arf: ActorRefFactory, shardName: String): ActorRef = arf match {
      case ctx: ActorContext ⇒ ClusterSharding(ctx.system).shardRegion(shardName)
      case sys: ActorSystem  ⇒ ClusterSharding(sys).shardRegion(shardName)
    }

  }

}
