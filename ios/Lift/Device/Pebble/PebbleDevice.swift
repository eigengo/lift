import Foundation

class PebbleDevice : NSObject, Device {
    internal let central = PBPebbleCentral.defaultCentral()
    internal let pebbleDeviceType = "pebble"
    
    private func getDeviceInfo(watch: PBWatch) -> DeviceInfo {
        return DeviceInfo.ConnectedDeviceInfo(id: watch.serialNumber.md5UUID(), type: pebbleDeviceType, name: watch.name, description: watch.serialNumber)
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
    
    func connect(deviceDelegate: DeviceDelegate, deviceSessionDelegate: DeviceSessionDelegate, onDone: ConnectedDevice -> Void) {
        onDone(PebbleConnectedDevice(deviceDelegate: deviceDelegate, deviceSessionDelegate: deviceSessionDelegate))
    }
    
}

/**
 * Pebble implementation of the ``Device`` protocol
 */
class PebbleConnectedDevice : PebbleDevice, PBPebbleCentralDelegate, PBWatchDelegate, ConnectedDevice {
    private var deviceDelegate: DeviceDelegate!
    private var deviceSessionDelegate: DeviceSessionDelegate!
    private var currentDeviceSession: PebbleDeviceSession?
    
    required init(deviceDelegate: DeviceDelegate, deviceSessionDelegate: DeviceSessionDelegate) {
        self.deviceDelegate = deviceDelegate
        self.deviceSessionDelegate = deviceSessionDelegate
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
            let deviceInfo = getDeviceInfo(watch)
            deviceDelegate.deviceGotDeviceInfo(deviceId, deviceInfo: deviceInfo)
            deviceDelegate.deviceAppLaunched(deviceId)
            currentDeviceSession?.stop()
            currentDeviceSession = PebbleDeviceSession(deviceId: deviceId, watch: watch, deviceSessionDelegate: deviceSessionDelegate)
        }
    }
    
    private func appKilled(watch: PBWatch!, error: NSError!) {
        currentDeviceSession?.stop()
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
        currentDeviceSession?.stop()
        // attempt to re-connect and launch
        start()
    }
    
}