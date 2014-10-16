import UIKit

class ViewController: UIViewController, PBDataLoggingServiceDelegate {
    
    @IBOutlet var name: UITextField!
    var count: UInt16?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        PBPebbleCentral.defaultCentral()!.dataLoggingService.delegate = self
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
 
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if segue.identifier == "record" {
            (segue.destinationViewController as RecordingControllerViewController).setName(name.text)
        }
    }
    
    func dataLoggingService(service: PBDataLoggingService!, hasByteArrays bytes: UnsafePointer<UInt8>, numberOfItems: UInt16, forDataLoggingSession session: PBDataLoggingSessionMetadata!) -> Bool {
        let type = UnsafePointer<Byte>(bytes)

        let dlHeader = UnsafePointer<dl_header>(bytes).getMirror()
        // TODO: check if dlHeader or gfsHeader received
        let gfsHeader = UnsafePointer<gfs_header>(bytes).getMirror()
        
        
        let gfsData = UnsafeMutablePointer<gfs_accel_data>.alloc(dlHeader.count)
        gfs_unpack_accel_data(bytes + sizeof(gfs_header), UInt16(dlHeader.count), gfsData)
        name.text = NSString(format: "received %d", numberOfItems)

        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        name.text = "Ended"
    }
    
}
