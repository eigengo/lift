import UIKit

class RecordingControllerViewController: UIViewController {
    private var recorder: MotionRecorder?

    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    func setName(name: String) {
        self.recorder = MotionRecorder(name: name)
        self.recorder!.startRecording { (count) -> Void in
            self.countLabel.text = String(count)
        }
    }
    
    @IBOutlet var countLabel: UILabel!
        
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if (self.recorder != nil) {
            self.recorder!.stopRecording()
        }
    }
    
}
