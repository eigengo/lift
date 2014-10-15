import UIKit

class ViewController: UIViewController, PBDataLoggingServiceDelegate {
    
    @IBOutlet var name: UITextField!
    
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
        name.text = NSString(format: "received %d", numberOfItems)

        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        name.text = "Ended"
    }
    
}