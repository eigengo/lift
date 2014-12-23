import Foundation

class PebbleDevice : NSObject, PBPebbleCentralDelegate, PBWatchDelegate {
    private let central = PBPebbleCentral.defaultCentral()
    private var watch: PBWatch?             // with launched app
    private var delegate: DeviceDelegate!
    
    required init(delegate: DeviceDelegate) {
        self.delegate = delegate
        super.init()
        
        let uuid = NSMutableData(length: 16)!
        NSUUID(UUIDString: "E113DED8-0EA6-4397-90FA-CE40941F7CBC")!.getUUIDBytes(UnsafeMutablePointer(uuid.mutableBytes))
        central.appUUID = uuid
        central.delegate = self
    }

    func launch() {
        if central.connectedWatches.count > 1 {
            delegate.deviceDidNotConnect(NSError.errorWithMessage("Device.Pebble.PebbleConnector.tooManyWatches", code: 1))
        } else if central.connectedWatches.count == 0 {
            delegate.deviceDidNotConnect(NSError.errorWithMessage("Device.Pebble.PebbleConnector.noWatches", code: 2))
        } else {
            let watch = central.connectedWatches[0] as PBWatch
            watch.appMessagesLaunch({ (watch: PBWatch!, error: NSError!) -> Void in
                watch.serialNumber
                if error != nil {
                    self.delegate.deviceAppLaunchFailed(watch.serialNumber.md5UUID(), error: error!)
                } else {
                    self.watch = watch
                    watch.versionInfo.hardwareVersion
                    
                    watch.serialNumber
                    
                    let deviceInfo = DeviceInfo(type: "Pebble",
                        name: watch.name,
                        serialNumber: watch.serialNumber,
                        address: String(format: "%@", watch.versionInfo.deviceAddress),
                        hardwareVersion: watch.versionInfo.hardwareVersion,
                        osVersion: watch.versionInfo.systemResources.friendlyVersion)
                    
                    self.delegate.deviceAppLaunched(watch.serialNumber.md5UUID(), deviceInfo: deviceInfo)
                }
            })
        }
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidConnect watch: PBWatch!, isNew: Bool) {
        NSLog("Connected %@", watch)
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidDisconnect watch: PBWatch!) {
        NSLog("watchDidDisconnect %@", watch)
        delegate.deviceDisconnected(watch.serialNumber.md5UUID())

        // remove our watch
        self.watch = nil
        
        // attempt to re-connect and launch
        launch()
    }
    
}