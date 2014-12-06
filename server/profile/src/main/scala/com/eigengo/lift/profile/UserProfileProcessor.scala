package com.eigengo.lift.profile

import java.security.MessageDigest
import java.util

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.profile.UserProfile.{UserDeviceSet, UserRegistered}
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

  /**
   * Sets the users' public profile
   * @param userId the user identity
   * @param publicProfile the public profile
   */
  case class UserSetPublicProfile(userId: UserId, publicProfile: PublicProfile)

  private case class KnownAccounts(accounts: Map[String, UserId]) {
    def contains(email: String): Boolean = accounts.contains(email)
    def get(email: String): Option[UserId] = accounts.get(email)
    def withNewAccount(email: String, userId: UserId): KnownAccounts = copy(accounts = accounts + (email → userId))
  }
  private object KnownAccounts {
    def empty: KnownAccounts = KnownAccounts(Map.empty)
  }

  private case class KnownAccountAdded(email: String, userId: UserId)

}

class UserProfileProcessor(userProfile: ActorRef) extends PersistentActor with ActorLogging {
  import com.eigengo.lift.profile.UserProfileProcessor._
  private var knownAccounts: KnownAccounts = KnownAccounts.empty
  private val mediator = DistributedPubSubExtension(context.system).mediator
  private val topic = "UserProfileProcessor.knownAccounts"
  mediator ! Subscribe(topic, self)

  private def digestPassword(password: String, salt: String): Array[Byte] = {
    val sha256 = MessageDigest.getInstance("SHA-256")
    sha256.digest((password + salt).getBytes)
  }

  private def loginFailed(sender: ActorRef): Unit = sender ! \/.left("Login failed 1")

  private def loginTry(sender: ActorRef, password: String)(userId: UserId): Unit = {
    import akka.pattern.ask
    import com.eigengo.lift.common.Timeouts.defaults._
    import context.dispatcher

    (userProfile ? UserGetAccount(userId)).mapTo[Account].foreach { account ⇒
      if (util.Arrays.equals(digestPassword(password, account.salt), account.password)) {
        sender ! \/.right(userId)
      } else {
        sender ! \/.left("Login failed 2")
      }
    }
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: KnownAccounts) ⇒
      knownAccounts = offeredSnapshot
  }

  override def receiveCommand: Receive = {
    case UserRegister(email, _) if knownAccounts.contains(email) ⇒
      log.info("UserRegister: username taken.")
      sender() ! \/.left("Username already taken")

    case UserRegister(email, password) if !knownAccounts.contains(email) ⇒
      log.info("UserRegister: username available.")
      val salt = Random.nextString(100)
      persist(UserRegistered(UserId.randomId(), Account(email, digestPassword(password, salt), salt))) { userRegistered ⇒
        userProfile ! userRegistered
        knownAccounts = knownAccounts.withNewAccount(email, userRegistered.userId)
        saveSnapshot(knownAccounts)
        mediator ! Publish(topic, KnownAccountAdded(email, userRegistered.userId))

        sender() ! \/.right(userRegistered.userId)
      }

    case KnownAccountAdded(email, userId) if sender() != self ⇒
      log.info(s"KnownAccountAdded. Accounts now ${knownAccounts.accounts}.")
      knownAccounts = knownAccounts.withNewAccount(email, userId)

    case UserLogin(email, password) ⇒
      log.info("UserLogin.")
      knownAccounts.get(email).fold(loginFailed(sender()))(loginTry(sender(), password))

    case UserSetDevice(userId, device) ⇒
      log.info("UserSetDevice.")
      userProfile ! UserDeviceSet(userId, device)
      sender() ! \/.right(())

    case UserSetPublicProfile(userId, publicProfile) ⇒
      log.info("UserSetPublicProfile.")
      userProfile ! UserPublicProfileSet(userId, publicProfile)
      sender() ! \/.right(())
  }

  override def persistenceId: String = "user-profile-processor"

}
