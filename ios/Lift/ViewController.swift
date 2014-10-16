import UIKit

class ViewController: UIViewController, PBDataLoggingServiceDelegate {
    
    @IBOutlet var nameLabel: UILabel!
    @IBOutlet var bytesLabel: UILabel!
    
    var samplesReceived: UInt = 0
    var bytesReceived: UInt = 0
    var start: NSTimeInterval?
    
    override func viewDidLoad() { 
        super.viewDidLoad()
        PBPebbleCentral.defaultCentral()!.dataLoggingService.delegate = self
        PBPebbleCentral.defaultCentral()!.dataLoggingService.setDelegateQueue(nil)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    func dataLoggingService(service: PBDataLoggingService!, hasByteArrays bytes: UnsafePointer<UInt8>, numberOfItems: UInt16, forDataLoggingSession session: PBDataLoggingSessionMetadata!) -> Bool {
        objc_sync_enter(self)
        bytesReceived = bytesReceived + UInt(numberOfItems)
        
        if start == nil {
            NSLog("start == 0")
            start = NSDate().timeIntervalSince1970
        }
        
        if numberOfItems > 0 {
            let gfsHeader = UnsafePointer<gfs_header>(bytes).memory
            samplesReceived = samplesReceived + UInt(gfsHeader.count)
            //gfs_unpack_accel_data(bytes + sizeof(gfs_header), gfsHeader.count, gfsData)
            nameLabel.text = NSString(format: "received %d %@", samplesReceived, session.serialNumber)
        }
        
        if start != nil {
            let elapsed = NSDate().timeIntervalSince1970 - start!
            let bytesPerSecond = Double(bytesReceived) / elapsed
            bytesLabel.text = NSString(format:"%d bytes,\n%f elapsed,\n%f bps", self.bytesReceived, elapsed, bytesPerSecond)
        }
        objc_sync_exit(self)
        
        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        nameLabel.text = NSString(format: "Ended %@", session.serialNumber)
    }
    
}
