package com.eigengo.lift.profile

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding

object UserProfileLink {
  private[profile] val userProfileShardName = "user-profile"

  def userProfile(system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = userProfileShardName,
      entryProps = UserProfile.shardingProps(),
      idExtractor = UserProfileProtocol.idExtractor,
      shardResolver = UserProfileProtocol.shardResolver)
  }
}
