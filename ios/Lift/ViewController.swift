import UIKit

class ViewController: UIViewController {
   
    @IBAction func poll(AnyObject) {
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
    }
    
}
