import Foundation

internal class PebbleDeviceSession {
    var id: NSUUID!
    var accelerometerStats: AccelerometerSessionStats?
    
    required init() {
        self.id = NSUUID()
    }
}

class PebbleDevice : NSObject, PBPebbleCentralDelegate, PBWatchDelegate {
    private let central = PBPebbleCentral.defaultCentral()
    private var deviceDelegate: DeviceDelegate!
    private var deviceDataDelegates: DeviceDataDelegates!
    private var updateHandler: AnyObject?
    
    private func accelerometerDataReceived(deviceSession: PebbleDeviceSession, data: NSData) {
        deviceDataDelegates.accelerometerDelegate.accelerometerReceiverReceived(deviceSession.id, data: data, stats: deviceSession.accelerometerStats!)
    }
    
    private func appMessagesReceived(deviceSession: PebbleDeviceSession, watch: PBWatch!, data: [NSObject : AnyObject]!) -> Bool {
        if let d: NSData = data[0xface0fb0] as? NSData {
            accelerometerDataReceived(deviceSession, d)
        }
        return true
    }
    
    private func versionInfoReceived(watch: PBWatch!, version: PBVersionInfo!) {
        let deviceInfoDetail = DeviceInfo.Detail(
            address: String(format: "%@", watch.versionInfo.deviceAddress),
            hardwareVersion: version.hardwareVersion,
            osVersion: version.systemResources.friendlyVersion)
        
        deviceDelegate.deviceGotDeviceInfoDetail(watch.serialNumber.md5UUID(), detail: deviceInfoDetail)
    }
    
    private func appLaunched(watch: PBWatch!, error: NSError!) {
        let deviceId = watch.serialNumber.md5UUID()
        if error != nil {
            deviceDelegate.deviceAppLaunchFailed(deviceId, error: error!)
        } else {
            updateHandler = watch.appMessagesAddReceiveUpdateHandler(appMessagesReceived(PebbleDeviceSession()))
            watch.getVersionInfo(versionInfoReceived, onTimeout: { (watch) -> Void in /* noop */ })
            
            let deviceInfo = DeviceInfo(type: "pebble",
                name: watch.name,
                serialNumber: watch.serialNumber)
            
            deviceDelegate.deviceGotDeviceInfo(deviceId, deviceInfo: deviceInfo)
            deviceDelegate.deviceAppLaunched(deviceId)
        }
    }
    
    required init(deviceDelegate: DeviceDelegate, deviceDataDelegates: DeviceDataDelegates) {
        self.deviceDelegate = deviceDelegate
        self.deviceDataDelegates = deviceDataDelegates
        super.init()
        
        let uuid = NSMutableData(length: 16)!
        NSUUID(UUIDString: "E113DED8-0EA6-4397-90FA-CE40941F7CBC")!.getUUIDBytes(UnsafeMutablePointer(uuid.mutableBytes))
        central.appUUID = uuid
        central.delegate = self
        
        start()
    }

    func start() {
        if central.connectedWatches.count > 1 {
            deviceDelegate.deviceDidNotConnect(NSError.errorWithMessage("Device.Pebble.PebbleConnector.tooManyWatches", code: 1))
        } else if central.connectedWatches.count == 0 {
            deviceDelegate.deviceDidNotConnect(NSError.errorWithMessage("Device.Pebble.PebbleConnector.noWatches", code: 2))
        } else {
            let watch = central.connectedWatches[0] as PBWatch
            watch.appMessagesLaunch(appLaunched)
        }
    }
    
    // --- PBPebbleCentralDelegate implementation ---
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidConnect watch: PBWatch!, isNew: Bool) {
        NSLog("Connected %@", watch)
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidDisconnect watch: PBWatch!) {
        NSLog("watchDidDisconnect %@", watch)
        deviceDelegate.deviceDisconnected(watch.serialNumber.md5UUID())
        if let x: AnyObject = updateHandler {
            watch.appMessagesRemoveUpdateHandler(x)
        }

        // attempt to re-connect and launch
        start()
    }
    
}