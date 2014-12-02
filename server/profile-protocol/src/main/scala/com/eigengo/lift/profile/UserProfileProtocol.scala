package com.eigengo.lift.profile

import akka.contrib.pattern.ShardRegion
import com.eigengo.lift.common.UserId

object UserProfileProtocol {

  val idExtractor: ShardRegion.IdExtractor = {
    case UserRegistered(userId, account) ⇒ (userId.toString, account)
    case UserGetProfile(userId)          ⇒ (userId.toString, GetProfile)
    case UserGetDevices(userId)          ⇒ (userId.toString, GetDevices)
    case UserDeviceSet(userId, device)   ⇒ (userId.toString, SetDevice(device))
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case UserRegistered(userId, _) ⇒ s"${userId.hashCode() % 10}"
    case UserGetProfile(userId)    ⇒ s"${userId.hashCode() % 10}"
    case UserGetDevices(userId)    ⇒ s"${userId.hashCode() % 10}"
    case UserDeviceSet(userId, _)  ⇒ s"${userId.hashCode() % 10}"
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
   * Device has been set
   * @param userId the user for the device
   * @param device the device that has just been set
   */
  case class UserDeviceSet(userId: UserId, device: UserDevice)

  /**
   * Get profile
   */
  case object GetProfile

  /**
   * Get all devices
   */
  case object GetDevices

  /**
   * All user devices
   * @param devices the devices
   */
  case class UserDevices(devices: Set[UserDevice]) extends AnyVal {
    def ::(device: UserDevice) = UserDevices(devices + device)
    def foreach[U](f: UserDevice ⇒ U): Unit = devices.foreach(f)
  }
  object UserDevices {
    /** empty UserDevices */
    val empty = UserDevices(Set.empty)
  }

  /**
   * The user profile includes the user's account and registered / known devices
   * @param account the account
   * @param devices the known devices
   */
  case class Profile(account: Account, devices: UserDevices) {
    /**
     * Adds a device to the profile
     * @param device the device
     * @return the updated profile
     */
    def addDevice(device: UserDevice) = copy(devices = device :: devices)
  }

  /**
   * The user account details
   * @param email the user's email
   * @param password the hashed password
   * @param salt the salt used in hashing
   */
  case class Account(email: String, password: Array[Byte], salt: String)

  /**
   * The user's devices
   */
  sealed trait UserDevice
  case class IOSUserDevice(deviceToken: String) extends UserDevice
  case class AndroidUserDevice() extends UserDevice

  /**
   * Get profile query for the given ``userId``
   * @param userId the user identity
   */
  case class UserGetProfile(userId: UserId)

  /**
   * Gets the user's devices
   * @param userId the user identity
   */
  case class UserGetDevices(userId: UserId)

}
