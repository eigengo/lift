import Foundation

typealias DeviceType = String

typealias DeviceId = NSUUID

enum DeviceInfo {
    /// type of device: pebble, applewatch, androidwear,...
    var type: DeviceType {
        get {
            switch self {
            case .ConnectedDeviceInfo(id: _, location: _, let t, _, _): return t
            case .DisconnectedDeviceInfo(_, location: _, let t, _): return t
            case .NotAvailableDeviceInfo(let t, location: _, _): return t
            }
        }
    }
    
    /// the location of the device
    var location: Location {
        get {
            switch self {
            case .ConnectedDeviceInfo(id: _, location: let l, type: _, name: _, serialNumber: _): return l
            case .DisconnectedDeviceInfo(id: _, location: let l, type: _, error: _): return l
            case .NotAvailableDeviceInfo(type: _, location: let l, error: _): return l
            }
        }
    }
    
    case ConnectedDeviceInfo(id: DeviceId, location: Location, type: DeviceType, name: String, serialNumber: String)
    
    case DisconnectedDeviceInfo(id: DeviceId, location: Location, type: DeviceType, error: NSError?)
    
    case NotAvailableDeviceInfo(type: DeviceType, location: Location, error: NSError)
    
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

final class DeviceSessionStats {
    private var stats: [DeviceSessionStats.Key : DeviceSessionStats.Entry] = [:]

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
        /// the location
        var location: DeviceInfo.Location
        
        var hashValue: Int {
            get {
                return sensorKind.hashValue + 21 * location.hashValue
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

    /**
    * Update the stats this session holds
    */
    final internal func updateStats(key: DeviceSessionStats.Key, update: DeviceSessionStats.Entry -> DeviceSessionStats.Entry) -> DeviceSessionStats.Entry {
        var prev: DeviceSessionStats.Entry
        let zero = DeviceSessionStats.Entry(bytes: 0, packets: 0)
        if let x = stats[key] { prev = x } else { prev = zero }
        let curr = update(prev)
        stats[key] = curr
        return curr
    }
    
    /**
     * Merge the statistics kept in this session with the statistics kept in ``that`` session
     */
    final internal func merge(that: DeviceSessionStats) {
        for (k, v) in that.stats {
            stats[k] = v
        }
    }
    
    internal func update(sensorKind: SensorKind, location: DeviceInfo.Location, update: DeviceSessionStats.Entry -> DeviceSessionStats.Entry) -> DeviceSessionStats.Entry {
        var prev: DeviceSessionStats.Entry
        let key = Key(sensorKind: sensorKind, location: location)
        let zero = DeviceSessionStats.Entry(bytes: 0, packets: 0)
        if let x = stats[key] { prev = x } else { prev = zero }
        let curr = update(prev)
        stats[key] = curr
        return curr
    }
    
    /**
    * Return the session stats as a list of tuples, ordered by the key
    */
    private func toList() -> [(DeviceSessionStats.Key, DeviceSessionStats.Entry)] {
        var r: [(DeviceSessionStats.Key, DeviceSessionStats.Entry)] = []
        for (k, v) in stats {
            r += [(k, v)]
        }
        return r
    }
    
    final subscript(index: Int) -> (DeviceSessionStats.Key, DeviceSessionStats.Entry) {
        return toList()[index]
    }
    
    var count: Int {
        get {
            return stats.count
        }
    }
    
}

func ==(lhs: DeviceSessionStats.Key, rhs: DeviceSessionStats.Key) -> Bool {
    return lhs.location == rhs.location && lhs.sensorKind == rhs.sensorKind
}

/**
 * The exercise session connected to the device
 */
class DeviceSession {
    internal var deviceInfo: DeviceInfo!
    internal var id: NSUUID!
    internal let stats: DeviceSessionStats = DeviceSessionStats()
    
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