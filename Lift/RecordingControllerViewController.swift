import UIKit

class RecordingControllerViewController: UIViewController {
    private let recorder: MotionRecorder = MotionRecorder()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

    @IBOutlet
    var markButton: UIButton!
    
    @IBAction
    func markButtonTouchUpInside(sender: NSObject) {
        self.recorder.mark()
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        self.recorder.stopRecording()
    }
    
}
