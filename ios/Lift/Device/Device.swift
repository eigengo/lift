import Foundation

///
/// Device type is a simple string: it also maps to the devices in the Images.xcassets
///
typealias DeviceType = String

///
/// Device id is a NSUUID
///
typealias DeviceId = NSUUID

///
/// All devices should send packets that are a certain number of samples long; and all devices 
/// should sample at the same frequency
///
struct DevicePace {
    ///
    /// The number of samples per packet
    ///
    static let samplesPerPacket = 124
}

///
/// Holds information about a device
///
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
    
    ///
    /// Indicates whether the device is connected
    ///
    var isConnected: Bool {
        get {
            switch self {
            case .ConnectedDeviceInfo(id: _, type: _, name: _, description: _): return true
            default: return false
            }
        }
    }
    
    case ConnectedDeviceInfo(id: DeviceId, type: DeviceType, name: String, description: String)
    
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
    
    /**
    * Sensor data location matching the Scala codebase
    */
    enum Location : UInt8 {
        case Wrist = 0x01
        case Waist = 0x02
        case Chest = 0x03
        case Foot  = 0x04
        case Any   = 0x7f
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
    func peek(onDone: DeviceInfo -> Void) -> Void
    
    /**
     * Connects the device and executes ``onDone`` when successful
     */
    func connect(deviceDelegate: DeviceDelegate, deviceSessionDelegate: DeviceSessionDelegate, onDone: ConnectedDevice -> Void) -> Void
    
}

/**
 * Common device communication protocol
 */
protocol ConnectedDevice {
    
    ///
    /// Starts the device work; typically also starts the companion app
    ///
    func start()
    
    ///
    /// Stops the device work; typically also stops the companion app
    ///
    func stop()
    
}

/**
 * The device delegate informs the caller about the connected device
 */
protocol DeviceDelegate {
        
    ///
    /// Information about the device is now available
    /// @param deviceId the device identity
    /// @param deviceInfo the device information
    ///
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo)
    
    ///
    /// Detailed information about the device is now available
    ///
    /// @param deviceId the device identity
    /// @param detail the detailed device information
    ///
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail)
    
    ///
    /// The device could not be connected. Maybe it's not registered, out of range, or anything
    /// else. The ``error`` contains the details
    ///
    /// @param error the failure detail
    ///
    func deviceDidNotConnect(error: NSError)
    
    ///
    /// The device is connected, but did not launch the companion app.
    ///
    /// @param deviceId the device identity
    /// @param error the failure detail
    ///
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError)
    
    ///
    /// The device is connected and successfully launched the companion app.
    ///
    /// @param deviceId the device identity
    ///
    func deviceAppLaunched(deviceId: DeviceId)
    
    ///
    /// A previously connected device disconnected unexpectedly. Out of range, batteries, ...
    ///
    /// @param deviceId the device identity
    ///
    func deviceDisconnected(deviceId: DeviceId)
    
}
