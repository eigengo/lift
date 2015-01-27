import Foundation

/**
 * Packs togehter multiple devices
 */
class MultiDevice : DeviceSession, SensorDataDelegate, DeviceDelegate {
    private var deviceInfos: [DeviceId : DeviceInfo] = [:]
    private var deviceInfoDetails: [DeviceId: DeviceInfo.Detail] = [:]
    private var deviceSessions: [DeviceId : DeviceSession] = [:]
    private var deviceData: [DeviceId : NSMutableData] = [:]
    private var devices: [ConnectedDevice] = []
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    private let deviceId = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000000")!
    
    private var sensorDataDelegate: SensorDataDelegate!
    private var deviceDelegate: DeviceDelegate!
    
    required init(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate) {
        // Multi device's ID is all zeros
        self.sensorDataDelegate = sensorDataDelegate
        self.deviceDelegate = deviceDelegate
        super.init()
        
        for device in Devices.devices {
            device.connect(self, sensorDataDelegate: self, onDone: { d in self.devices += [d] })
        }
    }
    
    ///
    /// Start all connected devices
    ///
    func start() -> Void {
        for d in devices { d.start() }
    }
    
    ///
    /// Get all device infos
    ///
    func getDeviceInfos() -> [(DeviceInfo, DeviceInfo.Detail?)] {
        var r: [(DeviceInfo, DeviceInfo.Detail?)] = []
        for (k, v) in deviceInfos {
            r += [(v, deviceInfoDetails[k])]
        }
        return r
    }
    
    ///
    /// Get the session stats at the given index
    ///
    func sessionStats(index: Int) -> (DeviceSessionStatsTypes.KeyWithLocation, DeviceSessionStatsTypes.Entry) {
        return combinedStats[index]
    }
    
    ///
    /// The connected device count
    ///
    var deviceCount: Int {
        get {
            return deviceInfos.count
        }
    }
    
    ///
    /// The session stats count
    ///
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
    
    func sensorDataEnded(deviceId: NSUUID, deviceSession: DeviceSession) {
        sensorDataDelegate.sensorDataEnded(deviceId, deviceSession: self)
    }
    
    func sensorDataReceived(deviceId: NSUUID, deviceSession: DeviceSession, data: NSData) {
        combinedStats.merge(deviceSession.getStats()) { k in
            let location = k.deviceId == ThisDevice.Info.id ? DeviceInfo.Location.Waist : DeviceInfo.Location.Wrist
            return DeviceSessionStatsTypes.KeyWithLocation(sensorKind: k.sensorKind, deviceId: k.deviceId, location: location)
        }
        sensorDataDelegate.sensorDataReceived(deviceId, deviceSession: self, data: NSData())
    }
}
