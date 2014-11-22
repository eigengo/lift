package com.eigengo.lift.profile

import akka.actor.Props
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.{AutoPassivation, UserId}

import scalaz.\/

object UserProfile {
  import com.eigengo.lift.profile.UserProfileProtocol._

  val shardName = "user-profile"
  val props = Props[UserProfile]
  
  val idExtractor: ShardRegion.IdExtractor = {
    case UserRegistered(userId, account) ⇒ (userId.toString, account)
    case UserGetProfile(userId)          ⇒ (userId.toString, GetProfile)
    case UserGetDevices(userId)          ⇒ (userId.toString, GetDevices)
    case UserSetDevice(userId, device)   ⇒ (userId.toString, SetDevice(device))
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case UserRegistered(userId, _) ⇒ s"${userId.hashCode() % 10}"
    case UserGetProfile(userId)    ⇒ s"${userId.hashCode() % 10}"
    case UserGetDevices(userId)    ⇒ s"${userId.hashCode() % 10}"
    case UserSetDevice(userId, _)  ⇒ s"${userId.hashCode() % 10}"
  }

  /**
   * Sets the user's device
   * @param device the device
   */
  case class SetDevice(device: UserDevice)

  /**
   * Registers a user
   * @param userId the user to be added
   * @param account the user account
   */
  case class UserRegistered(userId: UserId, account: Account)

  /**
   * Get profile
   */
  private case object GetProfile

  /**
   * Moves self to registered
   */
  private case object Registered

  /**
   * Get all devices
   */
  private case object GetDevices
}

/**
 * User profile domain
 */
class UserProfile extends PersistentActor with AutoPassivation {
  import com.eigengo.lift.profile.UserProfile._
  import com.eigengo.lift.profile.UserProfileProtocol._
  import scala.concurrent.duration._
  
  private var profile: Profile = _

  override def persistenceId: String = s"user-profile-${self.path.name}"

  override val passivationTimeout: Duration = 600.seconds

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Profile) ⇒
      profile = offeredSnapshot
      self ! Registered
  }

  override def receiveCommand: Receive = notRegistered

  private def notRegistered: Receive = withPassivation {
    case a: Account ⇒
      profile = Profile(a, UserDevices.empty)
      saveSnapshot(profile)
      self ! Registered
    case Registered ⇒
      context.become(registered)
  }

  private def registered: Receive = withPassivation {
    case SetDevice(device) ⇒
      profile = profile.addDevice(device)
      saveSnapshot(profile)
      sender() ! \/.right('OK)

    case GetProfile ⇒
      sender() ! profile
    case GetDevices ⇒
      sender() ! profile.devices
  }

}
