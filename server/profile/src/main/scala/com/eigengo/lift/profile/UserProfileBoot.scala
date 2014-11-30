package com.eigengo.lift.profile

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.ClusterSharding
import com.eigengo.lift.common.MicroserviceApp.BootedNode

import scala.concurrent.ExecutionContext

case class UserProfileBoot(userProfile: ActorRef, private val userProfileProcessor: ActorRef)
  extends UserProfileService with BootedNode {
  def route(ec: ExecutionContext) = userProfileRoute(userProfile, userProfileProcessor)(ec)
  override def api = Some(route)
}

object UserProfileBoot {

  def bootResolved(system: ActorSystem): UserProfileBoot = {
    val userProfile = ClusterSharding(system).start(
      typeName = UserProfileLink.userProfileShardName,
      entryProps = Some(UserProfile.props),
      idExtractor = UserProfile.idExtractor,
      shardResolver = UserProfile.shardResolver)
    val userProfileProcessor = system.actorOf(UserProfileProcessor.props(userProfile), UserProfileProcessor.name)

    UserProfileBoot(userProfile, userProfileProcessor)
  }

  def boot(system: ActorSystem): UserProfileBoot = bootResolved(system)

}
