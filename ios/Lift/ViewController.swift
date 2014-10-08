import UIKit

class ViewController: UIViewController {
    
    @IBOutlet var name: UITextField!
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
 
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if segue.identifier == "record" {
            (segue.destinationViewController as RecordingControllerViewController).setName(name.text)
        }
    }
    
}