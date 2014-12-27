import Foundation

typealias DeviceType = String

typealias DeviceId = NSUUID

enum DeviceInfo {
    /// type of device: pebble, applewatch, androidwear,...
    var type: DeviceType {
        get {
            switch self {
            case .ConnectedDeviceInfo(id: _, let t, _, _): return t
            case .DisconnectedDeviceInfo(_, let t, _): return t
            case .NotAvailableDeviceInfo(let t, _): return t
            }
        }
    }
    
    case ConnectedDeviceInfo(id: DeviceId, type: DeviceType, name: String, serialNumber: String)
    
    case DisconnectedDeviceInfo(id: DeviceId, type: DeviceType, error: NSError?)
    
    case NotAvailableDeviceInfo(type: DeviceType, error: NSError)
    
    ///TODO: Hmm!  
    ///case NotBoughtDeviceInfo(id: NSUUID, type: DeviceType, name: String, serialNumber: String)

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
* Common device communication protocol
*/
protocol Device {
    
    /**
    * Peeks the device for connectivity and information, potentially on another
    * queue, and calls ``onDone`` with the available information.
    *
    * @param onDone the function that will be called when the device information is available
    */
    func peek(onDone: DeviceInfo -> Void)
    
}

/**
 * Common device communication protocol
 */
protocol ConnectedDevice {
    
    /**
     * Starts the device work; typically also starts the companion app
     */
    func start()
    
    /**
     * Stops the device work; typically also stops the companion app
     */
    func stop()
    
    
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
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo)
    
    /**
     * Detailed information about the device is now available
     *
     * @param deviceId the device identity
     * @param detail the detailed device information
     */
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail)
    
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
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError)
    
    /**
     * The device is connected and successfully launched the companion app.
     *
     * @param deviceId the device identity
     */
    func deviceAppLaunched(deviceId: DeviceId)
    
    /**
     * A previously connected device disconnected unexpectedly. Out of range, batteries, ...
     *
     * @param deviceId the device identity
     */
    func deviceDisconnected(deviceId: DeviceId)
    
}