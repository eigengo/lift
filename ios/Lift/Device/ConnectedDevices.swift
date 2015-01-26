import Foundation

class ConnectedDevices : DeviceSession, SensorDataDelegate {
    private var deviceInfos: [DeviceId : DeviceInfo] = [:]
    private var deviceSessions: [DeviceId : DeviceSession] = [:]
    private var devices: [ConnectedDevice] = []
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    private var sensorDataDelegate: SensorDataDelegate!
    
    required init(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate) {
        let id = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000000")!
        self.sensorDataDelegate = sensorDataDelegate
        super.init(deviceInfo: DeviceInfo.ConnectedDeviceInfo(id: id, type: "", name: "", serialNumber: ""))
        
        for (device, info) in Devices.connectedDevices() {
            switch info {
            case .ConnectedDeviceInfo(id: let id, type: _, name: _, serialNumber: _): deviceInfos[id] = info
            default: fatalError("Connected device reports info != .ConnectedDeviceInfo")
            }
            device.connect(deviceDelegate, sensorDataDelegate: self, onDone: { d in self.devices += [d] })
        }
    }
    
    func start() -> Void {
        for d in devices { d.start() }
    }
    
    func deviceInfo(index: Int) -> DeviceInfo? {
        for (i, (_, let x)) in enumerate(deviceInfos) {
            if i == index {
                return x
            }
        }
        
        return nil
    }
    
    func sessionStats(index: Int) -> (DeviceSessionStatsTypes.KeyWithLocation, DeviceSessionStatsTypes.Entry) {
        return combinedStats[index]
    }
    
    var deviceCount: Int {
        get {
            return devices.count
        }
    }
    
    var statsCount: Int {
        get {
            return combinedStats.count
        }
    }
    
    // MARK: DeviceSession implementation
    override func stop() {
        for d in devices { d.stop() }
    }
    
    // MARK: SensorDataDelegate implementation
    
    func sensorDataEnded(deviceSession: DeviceSession) {
        sensorDataDelegate.sensorDataEnded(self)
    }
    
    func sensorDataReceived(deviceSession: DeviceSession, data: NSData) {
        combinedStats.merge(deviceSession.stats) { k in
            let location = k.deviceId == ThisDevice.Info.id ? DeviceInfo.Location.Waist : DeviceInfo.Location.Wrist
            return DeviceSessionStatsTypes.KeyWithLocation(sensorKind: k.sensorKind, deviceId: k.deviceId, location: location)
        }
        sensorDataDelegate.sensorDataReceived(self, data: NSData())
    }
}