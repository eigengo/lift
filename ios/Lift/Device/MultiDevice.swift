import Foundation

/**
 */
class MultiDevice : DeviceSession, SensorDataDelegate, DeviceDelegate {
    private var deviceInfos: [DeviceId : DeviceInfo] = [:]
    private var deviceInfoDetails: [DeviceId: DeviceInfo.Detail] = [:]
    private var deviceSessions: [DeviceId : DeviceSession] = [:]
    private var devices: [ConnectedDevice] = []
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    
    private var sensorDataDelegate: SensorDataDelegate!
    private var deviceDelegate: DeviceDelegate!
    
    required init(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate) {
        let id = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000000")!
        self.sensorDataDelegate = sensorDataDelegate
        self.deviceDelegate = deviceDelegate
        super.init(deviceInfo: DeviceInfo.ConnectedDeviceInfo(id: id, type: "", name: "", description: ""))
        
        for device in Devices.devices {
            device.connect(self, sensorDataDelegate: self, onDone: { d in self.devices += [d] })
        }
    }
    
    func start() -> Void {
        for d in devices { d.start() }
    }
    
    func getDeviceInfos() -> [(DeviceInfo, DeviceInfo.Detail?)] {
        var r: [(DeviceInfo, DeviceInfo.Detail?)] = []
        for (k, v) in deviceInfos {
            r += [(v, deviceInfoDetails[k])]
        }
        return r
    }
    
    func sessionStats(index: Int) -> (DeviceSessionStatsTypes.KeyWithLocation, DeviceSessionStatsTypes.Entry) {
        return combinedStats[index]
    }
    
    var deviceCount: Int {
        get {
            return deviceInfos.count
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
    
    // MARK: DeviceDelegate implementation
    func deviceAppLaunched(deviceId: DeviceId) {
        deviceDelegate.deviceAppLaunched(deviceId)
    }
    
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError) {
        deviceDelegate.deviceAppLaunchFailed(deviceId, error: error)
    }
    
    func deviceDidNotConnect(error: NSError) {
        deviceDelegate.deviceDidNotConnect(error)
    }
    
    func deviceDisconnected(deviceId: DeviceId) {
        deviceDelegate.deviceDisconnected(deviceId)
    }
    
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo) {
        deviceInfos[deviceId] = deviceInfo
        deviceDelegate.deviceGotDeviceInfo(deviceId, deviceInfo: deviceInfo)
    }
    
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail) {
        deviceInfoDetails[deviceId] = detail
        deviceDelegate.deviceGotDeviceInfoDetail(deviceId, detail: detail)
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
