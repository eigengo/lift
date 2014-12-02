package com.eigengo.lift.profile

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.AutoPassivation

object UserProfile {

  val props = Props[UserProfile]

}

/**
 * User profile domain
 */
class UserProfile extends PersistentActor with ActorLogging with AutoPassivation {
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
        profile = Profile(acc, UserDevices.empty)
        saveSnapshot(profile)
        context.become(registered)
      }
  }

  private def registered: Receive = withPassivation {
    case cmd@SetDevice(device) ⇒
      persist(cmd) { evt ⇒ profile = profile.addDevice(evt.device) }
      saveSnapshot(profile)

    case GetProfile ⇒
      sender() ! profile
    case GetDevices ⇒
      sender() ! profile.devices
  }

}
