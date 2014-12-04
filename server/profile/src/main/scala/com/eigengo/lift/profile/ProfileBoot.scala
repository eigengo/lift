package com.eigengo.lift.profile

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.ClusterSharding
import com.eigengo.lift.common.MicroserviceApp.BootedNode

import scala.concurrent.ExecutionContext

case class ProfileBoot(userProfile: ActorRef, private val userProfileProcessor: ActorRef)
  extends ProfileService with BootedNode {
  def route(ec: ExecutionContext) = userProfileRoute(userProfile, userProfileProcessor)(ec)
  override def api = Some(route)
}

object ProfileBoot {

  def boot(implicit system: ActorSystem): ProfileBoot = {
    val userProfile = ClusterSharding(system).start(
      typeName = UserProfile.shardName,
      entryProps = Some(UserProfile.props),
      idExtractor = UserProfile.idExtractor,
      shardResolver = UserProfile.shardResolver)
    val userProfileProcessor = system.actorOf(UserProfileProcessor.props(userProfile), UserProfileProcessor.name)

    ProfileBoot(userProfile, userProfileProcessor)
  }

}
