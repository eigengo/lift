package com.eigengo.lift.profile

import java.security.MessageDigest
import java.util

import akka.actor.{ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.profile.UserProfileProcessor.{UserLogin, UserRegister, UserSetDevice}
import com.eigengo.lift.profile.UserProfileProtocol._

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
  private var knownAccounts: Map[String, UserId] = Map.empty

  private def digestPassword(password: String, salt: String): Array[Byte] = {
    val sha256 = MessageDigest.getInstance("SHA-256")
    sha256.digest((password + salt).getBytes)
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: Map[String @unchecked, UserId @unchecked]) ⇒
      knownAccounts = offeredSnapshot
  }

  override def receiveCommand: Receive = {
    case UserRegister(email, password) if !knownAccounts.contains(email) ⇒
      val salt = Random.nextString(100)
      val userId = UserId.randomId()
      userProfile ! UserRegistered(userId, Account(email, digestPassword(password, salt), salt))
      knownAccounts = knownAccounts + (email → userId)
      saveSnapshot(knownAccounts)

      sender() ! \/.right(userId)
    case UserRegister(email, _) if knownAccounts.contains(email) ⇒
      sender() ! \/.left("Username already taken")

    case UserLogin(email, password) ⇒
      import akka.pattern.ask
      import com.eigengo.lift.common.Timeouts.defaults._
      import context.dispatcher

      knownAccounts.get(email).fold
        { sender() ! \/.left("Login failed 1") }
        { userId ⇒
          val sndr = sender()
          (userProfile ? UserGetProfile(userId)).mapTo[Profile].onSuccess {
            case profile ⇒
              if (util.Arrays.equals(
                digestPassword(password, profile.account.salt),
                profile.account.password)) {
                sndr ! \/.right(userId)
              } else {
                sndr ! \/.left("Login failed 2")
              }
          }
        }

    case UserSetDevice(userId, device) ⇒
      userProfile ! UserDeviceSet(userId, device)
      sender() ! \/.right(())
  }

  override def persistenceId: String = "user-profile-processor"

}
