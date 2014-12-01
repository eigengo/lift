package com.eigengo.lift.profile

import java.security.MessageDigest
import java.util.UUID

import akka.actor.{Props, ActorRef}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.profile.UserProfile.{UserDeviceSet, UserRegistered}
import com.eigengo.lift.profile.UserProfileProcessor.{UserLogin, UserSetDevice, UserRegister}
import com.eigengo.lift.profile.UserProfileProtocol.{UserDevice, Account}

import scala.collection.immutable.HashSet
import scala.util.Random
import scalaz.\/

object UserProfileProcessor {
  def props(userProfile: ActorRef) = Props(classOf[UserProfileProcessor], userProfile)
  val name = "user-profile-processor"

  /**
   * Registers the given email and password. Replies with ``\/[Err, UUID]``
   * @param email the email address
   * @param password the password
   */
  case class UserRegister(email: String, password: String)

  /**
   * Logins the given email and password. Replies with ``\/[Err, UUID]``
   * @param email the email address
   * @param password the password
   */
  case class UserLogin(email: String, password: String)

  /**
   * Add or update a device in the user's profile
   * @param userId the user identity
   * @param device the device to be added
   */
  case class UserSetDevice(userId: UserId, device: UserDevice)
}

class UserProfileProcessor(userProfile: ActorRef) extends PersistentActor {

  private var knownEmails: Set[String] = HashSet.empty

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Set[String @unchecked]) ⇒
      knownEmails = offeredSnapshot
  }

  override def receiveCommand: Receive = {
    case UserRegister(email, password) if !knownEmails.contains(email) ⇒
      val sha256 = MessageDigest.getInstance("SHA-256")
      val salt = Random.nextString(100)
      val userId = UserId.randomId()
      userProfile ! UserRegistered(userId, Account(email, sha256.digest((password + salt).getBytes), salt))
      knownEmails = knownEmails + email

      sender() ! \/.right(userId)
    case UserRegister(email, _) /* if knownEmails.contains(email) */ ⇒
      sender() ! \/.left("Username already taken")

    case UserLogin(email, password) ⇒
      sender() ! \/.right(UserId.randomId())

    case UserSetDevice(userId, device) ⇒
      userProfile ! UserDeviceSet(userId, device)
      sender() ! \/.right(())
  }

  override def persistenceId: String = "user-profile-processor"

}
