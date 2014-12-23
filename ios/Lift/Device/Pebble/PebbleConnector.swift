import Foundation

class PebbleConnector : NSObject, PBPebbleCentralDelegate, PBWatchDelegate {

    class var sharedInstance: PebbleConnector {
        struct Singleton {
            static let instance = PebbleConnector()
        }
        
        return Singleton.instance
    }
    
    private let central = PBPebbleCentral.defaultCentral()
    private var watch: PBWatch?       // with launched app
    
    override init() {
        super.init()
        
        let uuid = NSMutableData(length: 16)!
        NSUUID(UUIDString: "E113DED8-0EA6-4397-90FA-CE40941F7CBC")!.getUUIDBytes(UnsafeMutablePointer(uuid.mutableBytes))
        central.appUUID = uuid
        central.delegate = self
    }

    func launch() -> Result<Void> {
        if central.connectedWatches.count > 1 {
            Result.error(NSError.errorWithMessage("Device.Pebble.PebbleConnector.tooManyWatches", code: 1))
        } else if central.connectedWatches.count == 0 {
            Result.error(NSError.errorWithMessage("Device.Pebble.PebbleConnector.noWatches", code: 2))
        } else {
            let watch = central.connectedWatches[0] as PBWatch
            watch.appMessagesLaunch({ (watch: PBWatch!, error: NSError!) -> Void in
                if error != nil {
                    // TODO: something
                } else {
                    self.watch = watch
                }
            })
            Result.value(())
        }
    }
    
    private func launchLiftPebbleApp(watch: PBWatch!) -> Bool {
        watch.appMessagesLaunch({ (watch: PBWatch!, error: NSError!) -> Void in
            if (error != nil) {
                NSLog(":(")
            } else {
                NSLog(":)")
            }
            }, withUUID: PBPebbleCentral.defaultCentral().appUUID)
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidConnect watch: PBWatch!, isNew: Bool) {
        NSLog("Connected %@", watch)
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
    }
    
    func pebbleCentral(central: PBPebbleCentral!, watchDidDisconnect watch: PBWatch!) {
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
        NSLog("Gone %@", watch)
    }
    
}