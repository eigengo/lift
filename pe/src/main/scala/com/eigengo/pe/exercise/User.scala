package com.eigengo.pe.exercise

import akka.actor.{ActorRefFactory, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{SnapshotOffer, PersistentActor}
import com.eigengo.pe.{Actors, UserId}

object User {
  import com.eigengo.pe.push.PushNotification._
  
  case class Profile(pushNotifications: PushNotifications)
  object Profile {
    val empty = Profile(PushNotifications(None))
  }
  case class UserSetProfile(userId: UserId, preferences: Profile)

  case class PushNotifications(address: Option[NotificationAddress])
  case class UserSetPushNotifications(userId: UserId, pushNotifications: PushNotifications)

  val shardName = "user-shard"
  val props = Props[User]
  def lookup(implicit arf: ActorRefFactory) = Actors.shard.lookup(arf, shardName)
  
  val idExtractor: ShardRegion.IdExtractor = {
    case UserSetPushNotifications(userId, pushNotifications) ⇒ (userId.toString, pushNotifications)
    case UserSetProfile(userId, preferences) ⇒ (userId.toString, preferences)
  }
  
  val shardResolver: ShardRegion.ShardResolver = {
    case _ ⇒ "global"
  }
}

class User extends PersistentActor {
  import User._
  
  private var profile: Profile = Profile.empty

  private def setPushNotifications(pn: PushNotifications): Unit = {
    profile = profile.copy(pushNotifications = pn)
    saveSnapshot(profile)
  }

  private def setProfile(p: Profile): Unit = {
    profile = p
    saveSnapshot(profile)
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Profile) ⇒
      profile = offeredSnapshot
  }

  override def receiveCommand: Receive = {
    case pn: PushNotifications ⇒ persist(pn)(setPushNotifications)
    case p: Profile ⇒ persist(p)(setProfile)
  }

  override def persistenceId: String = s"user-${self.path.name}"
}
