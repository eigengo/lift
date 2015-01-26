import Foundation

class ConnectedDevices {
    private var deviceInfos: [DeviceId : (DeviceInfo, DeviceInfo.Detail?)] = [:]
    private var deviceSessions: [DeviceId : DeviceSession] = [:]
    private var devices: [ConnectedDevice] = []
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    
    required init(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate) {
        
    }
    
    var deviceCount: Int {
        get {
            return devices.count
        }
    }
    
    var stats: DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation> {
        get {
            return combinedStats
        }
    }
    
}