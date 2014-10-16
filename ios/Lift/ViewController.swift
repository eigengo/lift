import UIKit

class ViewController: UIViewController, PebbleAccelerometerReceiverDelegate {
    
    @IBOutlet var statusLabel: UILabel!
    private let receiver = PebbleAccelerometerReceiver()
    private let recorder = PebbleAccelerometerRecorder(name: "accel")
    
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
    
    func accelerometerReceiverReceived(data: NSData, bytesReceived: UInt, bytesPerSecond: Double) {
        recorder.accelerometerReceiverReceived(data, bytesReceived: bytesReceived, bytesPerSecond: bytesPerSecond)
        statusLabel.text = NSString(format: "Received %d\nBPS %f", bytesReceived, bytesPerSecond)
    }
        
}
