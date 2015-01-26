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
    func connect(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate, onDone: ConnectedDevice -> Void) -> Void
    
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

struct DeviceSessionStatsTypes {
    /**
    * The session statistics
    */
    struct Entry {
        /// total # bytes received
        var bytes: Int
        
        /// total # packets (i.e. group of accelerometer data) received
        var packets: Int
    }
    
    /**
    * The key in the stats
    */
    struct Key : Equatable, Hashable {
        /// the type
        var sensorKind: SensorKind
        /// the device identity
        var deviceId: DeviceId
        
        var hashValue: Int {
            get {
                return sensorKind.hashValue + 21 * deviceId.hashValue
            }
        }
    }
    
    struct KeyWithLocation : Equatable, Hashable {
        /// the type
        var sensorKind: SensorKind
        /// the device identity
        var deviceId: DeviceId
        /// the device location
        var location: DeviceInfo.Location
        
        var hashValue: Int {
            get {
                return sensorKind.hashValue + 21 * deviceId.hashValue
            }
        }
    }
    
    /**
    * The device stats keys
    */
    enum SensorKind {
        case Accelerometer
        case Gyroscope
        case GPS
        case HeartRate
    }
    
    
}

func ==(lhs: DeviceSessionStatsTypes.Key, rhs: DeviceSessionStatsTypes.Key) -> Bool {
    return lhs.deviceId == rhs.deviceId && lhs.sensorKind == rhs.sensorKind
}

func ==(lhs: DeviceSessionStatsTypes.KeyWithLocation, rhs: DeviceSessionStatsTypes.KeyWithLocation) -> Bool {
    return lhs.deviceId == rhs.deviceId && lhs.sensorKind == rhs.sensorKind && lhs.location == rhs.location
}

final class DeviceSessionStats<K : Hashable> {
    private var stats: [K : DeviceSessionStatsTypes.Entry] = [:]

    /**
    * Update the stats this session holds
    */
    final internal func updateStats(key: K, update: DeviceSessionStatsTypes.Entry -> DeviceSessionStatsTypes.Entry) -> DeviceSessionStatsTypes.Entry {
        var prev: DeviceSessionStatsTypes.Entry
        let zero = DeviceSessionStatsTypes.Entry(bytes: 0, packets: 0)
        if let x = stats[key] { prev = x } else { prev = zero }
        let curr = update(prev)
        stats[key] = curr
        return curr
    }
    
    /**
     * Merge the statistics kept in this session with the statistics kept in ``that`` session
     */
    final internal func merge<B>(that: DeviceSessionStats<B>, keyMapper: B -> K) {
        for (k, v) in that.stats {
            stats[keyMapper(k)] = v
        }
    }
    
    internal func update(key: K, update: DeviceSessionStatsTypes.Entry -> DeviceSessionStatsTypes.Entry) -> DeviceSessionStatsTypes.Entry {
        var prev: DeviceSessionStatsTypes.Entry
        let zero = DeviceSessionStatsTypes.Entry(bytes: 0, packets: 0)
        if let x = stats[key] { prev = x } else { prev = zero }
        let curr = update(prev)
        stats[key] = curr
        return curr
    }
    
    final subscript(index: Int) -> (K, DeviceSessionStatsTypes.Entry) {
        return stats.toList()[index]
    }
    
    var count: Int {
        get {
            return stats.count
        }
    }
    
}

/**
 * The exercise session connected to the device
 */
class DeviceSession {
    internal var deviceInfo: DeviceInfo!
    internal var id: NSUUID!
    internal let stats = DeviceSessionStats<DeviceSessionStatsTypes.Key>()
    
    ///
    /// Constructs a new session with generated identity
    ///
    init(deviceInfo: DeviceInfo) {
        self.id = NSUUID()
        self.deviceInfo = deviceInfo
    }
    
    ///
    /// Gets the device infor for the current session
    ///
    func getDeviceInfo() -> DeviceInfo {
        return deviceInfo
    }
    
    /**
     * Implementations must override this to handle stopping of the session
     */
    func stop() -> Void {
        fatalError("Implement me")
    }
    
    /**
     * Return the session identity
     */
    final func sessionId() -> NSUUID {
        return self.id
    }
        
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