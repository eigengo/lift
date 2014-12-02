package com.eigengo.lift.profile

import akka.actor.{ActorLogging, Props}
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
    case UserGetAccount(userId)          ⇒ (userId.toString, GetAccount)
    case UserGetPublicProfile(userId)    ⇒ (userId.toString, GetPublicProfile)
    case UserGetDevices(userId)          ⇒ (userId.toString, GetDevices)
    case UserDeviceSet(userId, device)   ⇒ (userId.toString, DeviceSet(device))
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case UserRegistered(userId, _)     ⇒ s"${userId.hashCode() % 10}"
    case UserGetAccount(userId)        ⇒ s"${userId.hashCode() % 10}"
    case UserGetDevices(userId)        ⇒ s"${userId.hashCode() % 10}"
    case UserDeviceSet(userId, _)      ⇒ s"${userId.hashCode() % 10}"
    case UserGetPublicProfile(userId)  ⇒ s"${userId.hashCode() % 10}"
  }

  /**
   * Sets the user's device
   * @param device the device
   */
  case class DeviceSet(device: UserDevice)

  /**
   * Registers a user
   * @param userId the user to be added
   * @param account the user account
   */
  case class UserRegistered(userId: UserId, account: Account)

  /**
   * Device has been set
   * @param userId the user for the device
   * @param device the device that has just been set
   */
  case class UserDeviceSet(userId: UserId, device: UserDevice)

  /**
   * Get account
   */
  private case object GetAccount

  /**
   * Get public profile
   */
  private case object GetPublicProfile

  /**
   * Get all devices
   */
  private case object GetDevices
}

/**
 * User profile domain
 */
class UserProfile extends PersistentActor with ActorLogging with AutoPassivation {
  import com.eigengo.lift.profile.UserProfile._
  import com.eigengo.lift.profile.UserProfileProtocol._
  import scala.concurrent.duration._

  private var profile: Profile = _

  override def persistenceId: String = s"user-profile-${self.path.name}"

  override val passivationTimeout: Duration = 10.seconds

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Profile) ⇒
      profile = offeredSnapshot
      context.become(registered)
  }

  override def receiveCommand: Receive = notRegistered

  private def notRegistered: Receive = withPassivation {
    case cmd: Account ⇒
      persist(cmd) { acc ⇒
        profile = Profile(acc, UserDevices.empty, None)
        saveSnapshot(profile)
        context.become(registered)
      }
  }

  private def registered: Receive = withPassivation {
    case cmd@DeviceSet(device) ⇒
      persist(cmd) { evt ⇒ profile = profile.addDevice(evt.device) }
      saveSnapshot(profile)

    case GetPublicProfile ⇒
      sender() ! profile.publicProfile
    case GetAccount ⇒
      sender() ! profile.account
    case GetDevices ⇒
      sender() ! profile.devices
  }

}
