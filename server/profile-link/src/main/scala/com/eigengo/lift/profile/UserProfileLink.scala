package com.eigengo.lift.profile

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding
import com.eigengo.lift.common.MicroserviceLink

object UserProfileLink {
  private[profile] val userProfileShardName = "user-profile"

  def userProfile(system: ActorSystem): ActorRef =
    ClusterSharding(system).start(userProfileShardName, None, UserProfileProtocol.idExtractor, UserProfileProtocol.shardResolver)

}
