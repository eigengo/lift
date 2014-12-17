package com.eigengo.lift.profile

import com.eigengo.lift.common.UserId

object UserProfileProtocol {

  import com.eigengo.lift.notification.NotificationProtocol._

  /**
   * The user profile includes the user's account and registered / known devices
   * @param account the account
   * @param devices the known devices
   * @param publicProfile the public profile
   */
  case class Profile(account: Account, devices: Devices, publicProfile: Option[PublicProfile]) {
    /**
     * Adds a device to the profile
     * @param device the device
     * @return the updated profile
     */
    def withDevice(device: Device) = copy(devices = device :: devices)

    /**
     * Sets the public profile
     * @param publicProfile the profile
     * @return the updated profile
     */
    def withPublicProfile(publicProfile: PublicProfile) = copy(publicProfile = Some(publicProfile))
  }

  /**
   * The user account details
   * @param email the user's email
   * @param password the hashed password
   * @param salt the salt used in hashing
   */
  case class Account(email: String, password: Array[Byte], salt: String)

  /**
   * User's public profile
   * @param firstName first name
   * @param lastName last name
   * @param weight weight
   * @param age age
   */
  case class PublicProfile(firstName: String, lastName: String, weight: Option[Int], age: Option[Int])

  /**
   * Get profile query for the given ``userId``
   * @param userId the user identity
   */
  case class UserGetAccount(userId: UserId)

  /**
   * Get the public account for the given ``userId``
   * @param userId the user identity
   */
  case class UserGetPublicProfile(userId: UserId)

  /**
   * Sets the public profile for the given ``userId``
   * @param userId the user identity
   * @param publicProfile the new public profile
   */
  case class UserPublicProfileSet(userId: UserId, publicProfile: PublicProfile)

  /**
   * Gets the user's devices
   * @param userId the user identity
   */
  case class UserGetDevices(userId: UserId)

}
