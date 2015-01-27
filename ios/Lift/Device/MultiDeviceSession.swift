import Foundation

///
/// Exposes information about a connected device
///
struct ConnectedDeviceInfo {
    enum Status {
        case WarmingUp
        case Ready
        case NotStarted
    }
    
    var deviceInfo: DeviceInfo
    var deviceInfoDetail: DeviceInfo.Detail?
    var status: Status
    
    func withDeviceInfo(deviceInfo: DeviceInfo) -> ConnectedDeviceInfo {
        return ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: deviceInfoDetail, status: status)
    }
    
    func withDeviceInfoDetail(deviceInfoDetail: DeviceInfo.Detail) -> ConnectedDeviceInfo {
        return ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: deviceInfoDetail, status: status)
    }
    
    func withStatus(status: Status) -> ConnectedDeviceInfo {
        return ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: deviceInfoDetail, status: status)
    }
}

///
/// Packs togehter multiple devices
///
class MultiDeviceSession : DeviceSession, DeviceSessionDelegate, DeviceDelegate {
    // the stats are combined for all devices
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    // our special deviceId is all 0s
    private let deviceId = DeviceId(UUIDString: "00000000-0000-0000-0000-000000000000")!

    private var deviceInfos: [DeviceId : ConnectedDeviceInfo] = [:]
    private var activeSessions: [DeviceId : DeviceSession] = [:]
    private var deviceData: [DeviceId : [NSData]] = [:]
    private var devices: [ConnectedDevice] = []
    private var warmedUp: Bool = false
    
    private var deviceSessionDelegate: DeviceSessionDelegate!
    private var deviceDelegate: DeviceDelegate!
    
    required init(deviceDelegate: DeviceDelegate, deviceSessionDelegate: DeviceSessionDelegate) {
        // Multi device's ID is all zeros
        self.deviceSessionDelegate = deviceSessionDelegate
        self.deviceDelegate = deviceDelegate
        super.init()
        
        for device in Devices.devices {
            device.connect(self, deviceSessionDelegate: self, onDone: { d in self.devices += [d] })
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
    func getDeviceInfo(index: Int) -> ConnectedDeviceInfo {
        return deviceInfos.values.array[index]
    }
    
    ///
    /// Get the session stats at the given index
    ///
    func getSessionStats(index: Int) -> (DeviceSessionStatsTypes.KeyWithLocation, DeviceSessionStatsTypes.Entry) {
        return combinedStats[index]
    }
    
    ///
    /// The connected device count
    ///
    var deviceInfoCount: Int {
        get {
            return deviceInfos.count
        }
    }
    
    ///
    /// The session stats count
    ///
    var sessionStatsCount: Int {
        get {
            return combinedStats.count
        }
    }
    
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
        deviceInfos.updated(deviceId, notFound: ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: nil, status: .NotStarted)) {
            $0.withDeviceInfo(deviceInfo)
        }
        deviceDelegate.deviceGotDeviceInfo(deviceId, deviceInfo: deviceInfo)
    }
    
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail) {
        deviceInfos.updated(deviceId) { $0.withDeviceInfoDetail(detail) }
        deviceDelegate.deviceGotDeviceInfoDetail(deviceId, detail: detail)
    }

    // MARK: DeviceSessionDelegate implementation
    func deviceSession(session: DeviceSession, sensorDataReceivedFrom deviceId: DeviceId, atDeviceTime time: CFAbsoluteTime, data: NSData) {
        if !warmedUp { return }
        if activeSessions[deviceId] == nil {
            NSLog("WARN: Ignoring sensorDataReceived from unknown device \(deviceId)")
            return
        }
        
        NSLog("INFO: Data from device \(deviceId.UUIDString)")
        
        if let ds = deviceData[deviceId] {
            deviceData[deviceId] = ds + [data]
        } else {
            deviceData[deviceId] = [data]
        }
        
        combinedStats.merge(session.getStats()) { k in
            let location = k.deviceId == ThisDevice.Info.id ? DeviceInfo.Location.Waist : DeviceInfo.Location.Wrist
            return DeviceSessionStatsTypes.KeyWithLocation(sensorKind: k.sensorKind, deviceId: k.deviceId, location: location)
        }
        deviceSessionDelegate.deviceSession(self, sensorDataReceivedFrom: deviceId, atDeviceTime: CFAbsoluteTimeGetCurrent(), data: data)
    }
    
    func deviceSession(deviceSession: DeviceSession, finishedWarmingUp deviceId: DeviceId) {
        deviceInfos.updated(deviceId) { $0.withStatus(.Ready) }
        activeSessions[deviceId] = deviceSession
        if activeSessions.count == devices.count {
            NSLog("INFO: all warmed up")
            for (_, v) in activeSessions {
                v.zero()
            }

            // we're ready
            warmedUp = true
            deviceSessionDelegate.deviceSession(self, finishedWarmingUp: self.deviceId)
        }
    }
    
    func deviceSession(deviceSession: DeviceSession, endedFrom deviceId: DeviceId) {
        deviceInfos.removeValueForKey(deviceId)
        activeSessions.removeValueForKey(deviceId)
        deviceSessionDelegate.deviceSession(self, endedFrom: deviceId)
    }
    
    func deviceSession(deviceSession: DeviceSession, sensorDataNotReceivedFrom deviceId: DeviceId) {
        // ???
    }
    
    func deviceSession(deviceSession: DeviceSession, startedWarmingUp deviceId: DeviceId, expectedCompletionIn time: NSTimeInterval) {
        deviceInfos.updated(deviceId) { $0.withStatus(.WarmingUp) }
    }
}


