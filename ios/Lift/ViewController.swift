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
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    func dataLoggingService(service: PBDataLoggingService!, hasByteArrays bytes: UnsafePointer<UInt8>, numberOfItems: UInt16, forDataLoggingSession session: PBDataLoggingSessionMetadata!) -> Bool {
        if (start == nil) {
            start = NSDate().timeIntervalSince1970
        }
        
        bytesReceived = bytesReceived + UInt(numberOfItems)
        if (numberOfItems > 0) {
            let gfsHeader = UnsafePointer<gfs_header>(bytes).memory
            
            let gfsData = UnsafeMutablePointer<gfs_accel_data>.alloc(Int(gfsHeader.count))
            samplesReceived = samplesReceived + UInt(gfsHeader.count)
            
            gfs_unpack_accel_data(bytes + sizeof(gfs_header), gfsHeader.count, gfsData)
            
            nameLabel.text = NSString(format: "received %d", samplesReceived)
        }
        
        if (start != nil) {
            let elapsed = NSDate().timeIntervalSince1970 - start!
            let bytesPerSecond = bytesReceived / UInt(elapsed)
            bytesLabel.text = NSString(format:"%d bytes, %%f elapsed, %d bps", self.bytesReceived)
        }
        
        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        nameLabel.text = "Ended"
    }
    
}
