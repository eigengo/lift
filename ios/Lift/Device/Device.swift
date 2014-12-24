import Foundation

/**
 * Device information structure
 */
struct DeviceInfo {
    /// type of device: pebble, applewatch, androidwear,...
    var type: String
    /// the device name, as reported by the device. Human readable: Pebble 4124, Apple Watch, ...
    var name: String
    /// the device serial or other identifier
    var serialNumber: String
    
    /**
     * The device details
     */
    struct Detail {
        /// typically BT address
        var address: String
        /// hardware version string
        var hardwareVersion: String
        /// OS version string
        var osVersion: String
    }
}

/**
 * Holds the delegates that react to the data received from the device. At the very least,
 * the ``AccelerometerDelegate`` must be set.
 *
 * In the future, other delegates may include heart rate, glucose, ...
 */
class DeviceDataDelegates {
    var accelerometerDelegate: AccelerometerDelegate
    
    required init(accelerometerDelegate: AccelerometerDelegate) {
        self.accelerometerDelegate = accelerometerDelegate
    }
}

/**
 * The session statistics
 */
struct DeviceSessionStats {
    /// total # bytes received
    var bytes: Int
    
    /// total # packets (i.e. group of accelerometer data) received
    var packets: Int
}

/**
 * The device stats keys
 */
enum DeviceSessionStatsKey {
    case Accelerometer
}

/**
 * The exercise session connected to the device
 */
protocol DeviceSession {

    /**
     * Return the session identity
     */
    func sessionId() -> NSUUID
    
    /**
     * Return the session stats as a list of tuples, ordered by the key
     */
    func sessionStats() -> [(DeviceSessionStatsKey, DeviceSessionStats)]
    
}

/**
 * The device delegate informs the caller about the connected device
 */
protocol DeviceDelegate {
    
    /**
     * Information about the device is now available
     *
     * @param deviceId the device identity
     * @param deviceInfo the device information
     */
    func deviceGotDeviceInfo(deviceId: NSUUID, deviceInfo: DeviceInfo)
    
    /**
     * Detailed information about the device is now available
     *
     * @param deviceId the device identity
     * @param detail the detailed device information
     */
    func deviceGotDeviceInfoDetail(deviceId: NSUUID, detail: DeviceInfo.Detail)
    
    /**
     * The device could not be connected. Maybe it's not registered, out of range, or anything
     * else. The ``error`` contains the details
     *
     * @param error the failure detail
     */
    func deviceDidNotConnect(error: NSError)
    
    /**
     * The device is connected, but did not launch the companion app.
     *
     * @param deviceId the device identity
     * @param error the failure detail
     */
    func deviceAppLaunchFailed(deviceId: NSUUID, error: NSError)
    
    /**
     * The device is connected and successfully launched the companion app.
     *
     * @param deviceId the device identity
     */
    func deviceAppLaunched(deviceId: NSUUID)
    
    /**
     * A previously connected device disconnected unexpectedly. Out of range, batteries, ...
     *
     * @param deviceId the device identity
     */
    func deviceDisconnected(deviceId: NSUUID)
    
}