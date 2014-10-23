import UIKit

class ViewController: UIViewController, PebbleAccelerometerReceiverDelegate {
    
    @IBOutlet var statusLabel: UILabel!
    private let receiver = PebbleAccelerometerReceiver()
    private let recorder = PebbleAccelerometerRecorder()
    
    override func viewDidLoad() { 
        super.viewDidLoad()
        receiver.delegate = self
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    func accelerometerReceiverEnded() {
        statusLabel.text = "Ended"
        recorder.accelerometerReceiverEnded()
    }
    
    func accelerometerReceiverReceived(data: NSData, bytesReceived: UInt, bytesPerSecond: Double, session: PBDataLoggingSessionMetadata!) {
        recorder.accelerometerReceiverReceived(data, bytesReceived: bytesReceived, bytesPerSecond: bytesPerSecond, session: session)
        statusLabel.text = NSString(format: "Session: %@\nReceived %d\nBPS %f", session.description, bytesReceived, bytesPerSecond)
    }
        
}
