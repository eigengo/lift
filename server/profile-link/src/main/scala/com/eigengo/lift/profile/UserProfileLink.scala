package com.eigengo.lift.profile

import akka.actor.{ActorRef, ActorSystem}
import com.eigengo.lift.common.MicroserviceLink

object UserProfileLink extends MicroserviceLink {
  private[profile] val userProfileShardName = "user-profile"

  def userProfile(implicit system: ActorSystem): ActorRef = clusterShardedLink(userProfileShardName)

}
