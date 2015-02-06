import Foundation

///
/// Exposes information about a connected device
///
struct ConnectedDeviceInfo {
    var deviceInfo: DeviceInfo
    var deviceInfoDetail: DeviceInfo.Detail?
    
    func withDeviceInfo(deviceInfo: DeviceInfo) -> ConnectedDeviceInfo {
        return ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: deviceInfoDetail)
    }
    
    func withDeviceInfoDetail(deviceInfoDetail: DeviceInfo.Detail) -> ConnectedDeviceInfo {
        return ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: deviceInfoDetail)
    }
}

///
/// Packs togehter multiple devices
///
class MultiDeviceSession : DeviceSession, DeviceSessionDelegate, DeviceDelegate, SensorDataGroupBufferDelegate {
    // the stats are combined for all devices
    private let combinedStats = DeviceSessionStats<DeviceSessionStatsTypes.KeyWithLocation>()
    // our special deviceId is all 0s
    private let multiDeviceId = DeviceId(UUIDString: "00000000-0000-0000-0000-000000000000")!

    private var deviceInfos: [DeviceId : ConnectedDeviceInfo] = [:]
    private var devices: [ConnectedDevice] = []
    
    private var sensorDataGroupBuffer: SensorDataGroupBuffer!
    private let deviceSessionDelegate: DeviceSessionDelegate!
    private let deviceDelegate: DeviceDelegate!
    
    required init(deviceDelegate: DeviceDelegate, deviceSessionDelegate: DeviceSessionDelegate) {
        // Multi device's ID is all zeros
        self.deviceSessionDelegate = deviceSessionDelegate
        self.deviceDelegate = deviceDelegate
        super.init()
        
        self.sensorDataGroupBuffer = SensorDataGroupBuffer(delegate: self, queue: dispatch_get_main_queue(), deviceLocations: { LiftUserDefaults.getLocation(deviceId: $0) })
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
        sensorDataGroupBuffer.stop()
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
        deviceInfos.updated(deviceId, notFound: ConnectedDeviceInfo(deviceInfo: deviceInfo, deviceInfoDetail: nil)) {
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
        sensorDataGroupBuffer.decodeAndAdd(data, fromDeviceId: deviceId, maximumGap: 0.3, gapValue: 0)
        
        combinedStats.merge(session.getStats()) { k in
            let location = k.deviceId == ThisDevice.Info.id ? DeviceInfo.Location.Waist : DeviceInfo.Location.Wrist
            return DeviceSessionStatsTypes.KeyWithLocation(sensorKind: k.sensorKind, deviceId: k.deviceId, location: location)
        }
    }
    
    func deviceSession(deviceSession: DeviceSession, endedFrom deviceId: DeviceId) {
        deviceInfos.removeValueForKey(deviceId)
        deviceSessionDelegate.deviceSession(self, endedFrom: deviceId)
    }
    
    func deviceSession(deviceSession: DeviceSession, sensorDataNotReceivedFrom deviceId: DeviceId) {
        // ???
    }
    
    // MARK: SensorDataGroupBufferDelegate implementation
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedAt time: CFAbsoluteTime, data: NSData) {
        deviceSessionDelegate.deviceSession(self, sensorDataReceivedFrom: multiDeviceId, atDeviceTime: CFAbsoluteTimeGetCurrent(), data: data)
    }
    
}


