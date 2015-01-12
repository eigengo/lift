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
   * @param lastRegisteredDevice the last device to have been added to the set
   */
  case class Devices(devices: Set[Device], lastRegisteredDevice: Option[Device]) {
    lazy val justLast: Devices = lastRegisteredDevice.map(Devices.one).getOrElse(Devices.empty)

    def withNewDevice(device: Device) = copy(devices + device, lastRegisteredDevice = Some(device))
    def foreach[U](f: Device ⇒ U): Unit = devices.foreach(f)
  }
  object Devices {
    /** empty UserDevices */
    val empty = Devices(Set.empty, None)
    def one(device: Device): Devices = Devices(Set(device), Some(device))
  }

  /**
   * Payloads to be sent over push notification mechanism
   */
  sealed trait PushMessagePayload

  /**
   * Message that should appear on the screen.
   * @param message the message
   * @param badge the badge
   * @param sound the sound
   */
  case class ScreenMessagePayload(message: String, badge: Option[Int], sound: Option[String]) extends PushMessagePayload

  /**
   * Message that delivers some data to the application
   * @param message the message
   */
  case class DataMessagePayload(message: Any) extends PushMessagePayload

  /**
   * Sends default message to the client
   *
   * @param devices the devices to send the message to
   * @param payload the message payload
   */
  case class PushMessage(devices: Devices, payload: PushMessagePayload)

}
