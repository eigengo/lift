package com.eigengo.lift.notification

import java.util

object NotificationProtocol {

  /**
   * The user's devices
   */
  sealed trait Device
  case class IOSDevice(deviceToken: Array[Byte]) extends Device {
    override def equals(obj: scala.Any): Boolean = obj match {
      case IOSDevice(dt) ⇒ util.Arrays.equals(deviceToken, dt)
      case x ⇒ false
    }

    override val hashCode: Int = deviceToken.hashCode()
  }
  case class AndroidDevice() extends Device

  /**
   * All user devices
   * @param devices the devices
   */
  case class Devices(devices: Set[Device]) extends AnyVal {
    def ::(device: Device) = Devices(devices + device)
    def foreach[U](f: Device ⇒ U): Unit = devices.foreach(f)
  }
  object Devices {
    /** empty UserDevices */
    val empty = Devices(Set.empty)
  }

  sealed trait Destination
  case object MobileDestination extends Destination
  case object WatchDestination extends Destination
  
  /**
   * Sends default message to the client
   *
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class PushMessage(devices: Devices, message: String, badge: Option[Int], sound: Option[String], destinations: Seq[Destination])

}
