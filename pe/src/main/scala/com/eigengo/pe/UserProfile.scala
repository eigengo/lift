package com.eigengo.pe

import java.util.UUID

/**
 * UserId type
 * @param id the underlying id
 */
case class UserId(id: UUID) extends AnyVal {
  override def toString() = id.toString
}
object UserId {
  def apply(s: String): UserId = UserId(UUID.fromString(s))
}


/**
 * The user identity carrying the primary identity and the user's current location
 * @param id the user identity
 * @param location the geolocation in long, lat
 */
case class UserIdAndLocation(id: UserId, location: Option[(Double, Double)] = None)
