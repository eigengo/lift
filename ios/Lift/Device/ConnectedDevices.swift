import Foundation

class ConnectedDevices : DeviceSession, SensorDataDelegate {
    private var deviceInfos: [DeviceId : (DeviceInfo, DeviceInfo.Detail?)] = [:]
    private var deviceSessions: [DeviceId : DeviceSession] = [:]
    private var devices: [ConnectedDevice] = []
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    private var sensorDataDelegate: SensorDataDelegate!
    
    required init(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate) {
        let id = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000000")!
        self.sensorDataDelegate = sensorDataDelegate
        super.init(deviceInfo: DeviceInfo.ConnectedDeviceInfo(id: id, type: "", name: "", serialNumber: ""))
                
        for (device, info) in Devices.connectedDevices() {
            device.connect(deviceDelegate, sensorDataDelegate: self, onDone: const(()))
        }
    }
    
    func start() -> Void {
        
    }
    
    func deviceInfo(index: Int) -> (DeviceInfo, DeviceInfo.Detail?)? {
        for (i, (_, (let x))) in enumerate(deviceInfos) {
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
        
    }
    
    // MARK: SensorDataDelegate implementation
    
    func sensorDataEnded(deviceSession: DeviceSession) {
        sensorDataDelegate.sensorDataEnded(self)
    }
    
    func sensorDataReceived(deviceSession: DeviceSession, data: NSData) {
        sensorDataDelegate.sensorDataReceived(self, data: NSData())
    }
}