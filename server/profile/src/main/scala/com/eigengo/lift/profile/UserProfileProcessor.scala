package com.eigengo.lift.profile

import java.security.MessageDigest

import akka.actor.{Props, ActorRef}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.profile.UserProfile.UserRegistered
import com.eigengo.lift.profile.UserProfileProcessor.UserRegister
import com.eigengo.lift.profile.UserProfileProtocol.Account

import scala.collection.immutable.HashSet
import scala.util.Random
import scalaz.\/

object UserProfileProcessor {
  def props(userProfile: ActorRef) = Props(classOf[UserProfileProcessor], userProfile)
  val name = "user-profile-processor"

  case class UserRegister(email: String, password: String)
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

      // TODO: stick with \/
      sender() ! userId
    case UserRegister(email, _) /* if knownEmails.contains(email) */ ⇒
      sender() ! \/.left("Username already taken")
  }

  override def persistenceId: String = "user-profile-processor"

}
