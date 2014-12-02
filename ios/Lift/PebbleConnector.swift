import Foundation

public class PebbleConnector : NSObject, PBPebbleCentralDelegate, PBWatchDelegate {

    public class var sharedInstance: PebbleConnector {
        struct Singleton {
            static let instance = PebbleConnector()
        }
        
        return Singleton.instance
    }
    
    let central = PBPebbleCentral.defaultCentral()
    
    override init() {
        super.init()
        
        let uuid = NSMutableData(length: 16)!
        NSUUID(UUIDString: "E113DED8-0EA6-4397-90FA-CE40941F7CBC")!.getUUIDBytes(UnsafeMutablePointer(uuid.mutableBytes))
        central.appUUID = uuid
        central.delegate = self
    }

    public func launch() {
        for w in central.connectedWatches {
            launchLiftPebbleApp(w as PBWatch)
        }
    }
    
    func launchLiftPebbleApp(watch: PBWatch!) {
        watch.appMessagesLaunch({ (watch: PBWatch!, error: NSError!) -> Void in
            if (error != nil) {
                NSLog(":(")
            } else {
                NSLog(":)")
            }
            }, withUUID: PBPebbleCentral.defaultCentral().appUUID)
    }
    
    public func pebbleCentral(central: PBPebbleCentral!, watchDidConnect watch: PBWatch!, isNew: Bool) {
        NSLog("Connected %@", watch)
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
    }
    
    public func pebbleCentral(central: PBPebbleCentral!, watchDidDisconnect watch: PBWatch!) {
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
        NSLog("Gone %@", watch)
    }
    
}