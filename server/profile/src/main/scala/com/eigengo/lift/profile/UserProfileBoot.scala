package com.eigengo.lift.profile

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.ClusterSharding

import scala.concurrent.ExecutionContext

case class UserProfileBoot(userProfile: ActorRef, private val userProfileProcessor: ActorRef) extends UserProfileService {
  def route(implicit ec: ExecutionContext) = userProfileRoute(userProfile, userProfileProcessor)
}

object UserProfileBoot {

  def boot(implicit system: ActorSystem): UserProfileBoot = {
    val userProfile = ClusterSharding(system).start(
      typeName = UserProfile.shardName,
      entryProps = Some(UserProfile.props),
      idExtractor = UserProfile.idExtractor,
      shardResolver = UserProfile.shardResolver)
    val userProfileProcessor = system.actorOf(UserProfileProcessor.props(userProfile), UserProfileProcessor.name)

    UserProfileBoot(userProfile, userProfileProcessor)
  }

}
