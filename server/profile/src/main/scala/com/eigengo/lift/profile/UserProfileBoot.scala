package com.eigengo.lift.profile

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.ClusterSharding
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import scala.concurrent.ExecutionContext
import scala.collection.JavaConversions._

case class UserProfileBoot(userProfile: ActorRef, private val userProfileProcessor: ActorRef)
  extends UserProfileService with BootedNode {
  def route(ec: ExecutionContext) = userProfileRoute(userProfile, userProfileProcessor)(ec)
  override def api = Some(route)
}

object UserProfileBoot {

  def boot(system: ActorSystem): UserProfileBoot = {
    val roles = system.settings.config.getStringList("akka.cluster.roles")
    val userProfile = ClusterSharding(system).start(
      typeName = UserProfileLink.userProfileShardName,
      entryProps = roles.find("profile" ==).map(_ => UserProfile.props),
      idExtractor = UserProfileProtocol.idExtractor,
      shardResolver = UserProfileProtocol.shardResolver)
    val userProfileProcessor = system.actorOf(UserProfileProcessor.props(userProfile), UserProfileProcessor.name)

    UserProfileBoot(userProfile, userProfileProcessor)
  }

}
