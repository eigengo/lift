import Foundation

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
    final func updateStats(key: K, update: DeviceSessionStatsTypes.Entry -> DeviceSessionStatsTypes.Entry) -> DeviceSessionStatsTypes.Entry {
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
    final func merge<B>(that: DeviceSessionStats<B>, keyMapper: B -> K) {
        for (k, v) in that.stats {
            stats[keyMapper(k)] = v
        }
    }
    
    func update(key: K, update: DeviceSessionStatsTypes.Entry -> DeviceSessionStatsTypes.Entry) -> DeviceSessionStatsTypes.Entry {
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

