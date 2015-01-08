import Foundation

/**
 * Session receiving app updates from the watch. When initialised with ``PBWatch``, it sets all required handlers
 * and receives the incoming data from the device.
 *
 * It also maintains required stats.
 */
class PebbleDeviceSession : DeviceSession {
    private var id: NSUUID!
    private var stats: [DeviceSessionStatsKey : DeviceSessionStats] = [:]
    private var deviceDataDelegates: DeviceDataDelegates!
    private var startTime: NSDate!
    private var updateHandler: AnyObject?

    required init(watch: PBWatch!, deviceDataDelegates: DeviceDataDelegates) {
        self.id = NSUUID()
        self.startTime = NSDate()
        self.deviceDataDelegates = deviceDataDelegates
        self.updateHandler = watch.appMessagesAddReceiveUpdateHandler(appMessagesReceiveUpdateHandler)
    }
    
    // MARK: DeviceSession
    
    func sessionId() -> NSUUID {
        return self.id
    }
    
    func sessionStats() -> [(DeviceSessionStatsKey, DeviceSessionStats)] {
        var r: [(DeviceSessionStatsKey, DeviceSessionStats)] = []
        for (k, v) in stats {
            r += [(k, v)]
        }
        return r
    }

    // MARK: main
    
    internal func stop(watch: PBWatch!) {
        if let x: AnyObject = updateHandler { watch.appMessagesRemoveUpdateHandler(x) }
        deviceDataDelegates.accelerometerDelegate.accelerometerDataEnded(self)
    }
    
    private func appMessagesReceiveUpdateHandler(watch: PBWatch!, data: [NSObject : AnyObject]!) -> Bool {
        let adKey = NSNumber(uint32: 0xface0fb0)
        let deadKey = NSNumber(uint32: 0x0000dead)
        if let x = data[adKey] as? NSData {
            accelerometerDataReceived(x)
        } else if let x: AnyObject = data[deadKey] {
            stop(watch)
        }
        return true
    }
    
    private func updateStats(key: DeviceSessionStatsKey, update: DeviceSessionStats -> DeviceSessionStats) -> DeviceSessionStats {
        var prev: DeviceSessionStats
        let zero = DeviceSessionStats(bytes: 0, packets: 0)
        if let x = stats[key] { prev = x } else { prev = zero }
        let curr = update(prev)
        stats[key] = curr
        return curr
    }

    private func accelerometerDataReceived(data: NSData) {
        let stats = updateStats(.Accelerometer, update: { prev in
            return DeviceSessionStats(bytes: prev.bytes + data.length, packets: prev.packets + 1)
        })
        
        deviceDataDelegates.accelerometerDelegate.accelerometerDataReceived(self, data: data)
    }
}

class PebbleDevice : NSObject, Device {
    internal let central = PBPebbleCentral.defaultCentral()
    internal let pebbleDeviceType = "pebble"
    
    private func getDeviceInfo(watch: PBWatch) -> DeviceInfo {
        return DeviceInfo.ConnectedDeviceInfo(id: watch.serialNumber.md5UUID(), type: pebbleDeviceType, name: watch.name, serialNumber: watch.serialNumber)
    }
    
    // MARK: Device implementation
    
    internal func findWatch() -> Either<NSError, PBWatch> {
        if central.connectedWatches.count > 1 {
            return Either.left(NSError.errorWithMessage("Device.Pebble.tooManyWatches".localized(), code: 1))
        } else if central.connectedWatches.count == 0 {
            return Either.left(NSError.errorWithMessage("Device.Pebble.noWatches".localized(), code: 2))
        } else {
            let watch = central.connectedWatches[0] as PBWatch
            return Either.right(watch)
        }
    }
    
    func peek(onDone: DeviceInfo -> Void) {
        findWatch().either({ err in onDone(DeviceInfo.NotAvailableDeviceInfo(type: self.pebbleDeviceType, error: err)) },
            onR: { watch in onDone(self.getDeviceInfo(watch)) })
    }
    
}

/**
 * Pebble implementation of the ``Device`` protocol
 */
class PebbleConnectedDevice : PebbleDevice, PBPebbleCentralDelegate, PBWatchDelegate, ConnectedDevice {
    private var deviceDelegate: DeviceDelegate!
    private var deviceDataDelegates: DeviceDataDelegates!
    private var currentDeviceSession: PebbleDeviceSession?
    
    required init(deviceDelegate: DeviceDelegate, deviceDataDelegates: DeviceDataDelegates) {
        self.deviceDelegate = deviceDelegate
        self.deviceDataDelegates = deviceDataDelegates
        super.init()
        
        let uuid = NSMutableData(length: 16)!
        NSUUID(UUIDString: "E113DED8-0EA6-4397-90FA-CE40941F7CBC")!.getUUIDBytes(UnsafeMutablePointer(uuid.mutableBytes))
        central.appUUID = uuid
        central.delegate = self
    }

    ///
    /// Version info callback from the watch
    ///
    private func versionInfoReceived(watch: PBWatch!, version: PBVersionInfo!) {
        let deviceInfoDetail = DeviceInfo.Detail(
            address: String(format: "%@", watch.versionInfo.deviceAddress),
            hardwareVersion: version.hardwareVersion,
            osVersion: version.systemResources.friendlyVersion)
        
        deviceDelegate.deviceGotDeviceInfoDetail(watch.serialNumber.md5UUID(), detail: deviceInfoDetail)
    }
    
    ///
    /// App launched callback from the watch
    ///
    private func appLaunched(watch: PBWatch!, error: NSError!) {
        let deviceId = watch.serialNumber.md5UUID()
        if error != nil {
            deviceDelegate.deviceAppLaunchFailed(deviceId, error: error!)
        } else {
            watch.getVersionInfo(versionInfoReceived, onTimeout: { (watch) -> Void in /* noop */ })
            deviceDelegate.deviceGotDeviceInfo(deviceId, deviceInfo: getDeviceInfo(watch))
            deviceDelegate.deviceAppLaunched(deviceId)
            currentDeviceSession?.stop(watch)
            currentDeviceSession = PebbleDeviceSession(watch: watch, deviceDataDelegates: deviceDataDelegates)
        }
    }
    
    private func appKilled(watch: PBWatch!, error: NSError!) {
        currentDeviceSession?.stop(watch)
        currentDeviceSession = nil
    }
    
    // MARK: Device implementation

    func start() {
        findWatch().either({ x in self.deviceDelegate.deviceDidNotConnect(x) }, onR: { $0.appMessagesLaunch(self.appLaunched) })
    }
    
    func stop() {
        findWatch().either({ x in self.deviceDelegate.deviceDidNotConnect(x) }, onR: { $0.appMessagesKill(self.appKilled) })
    }
    
    // MARK: PBPebbleCentralDelegate implementation
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidConnect watch: PBWatch!, isNew: Bool) {
        NSLog("Connected %@", watch)
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidDisconnect watch: PBWatch!) {
        NSLog("watchDidDisconnect %@", watch)
        deviceDelegate.deviceDisconnected(watch.serialNumber.md5UUID())
        currentDeviceSession?.stop(watch)
        // attempt to re-connect and launch
        start()
    }
    
}