import UIKit

class LiftAlertController: UIViewController {
    var messageText: String?

    class func error(message: String, error: NSError) -> LiftAlertController {
        let ctrl = LiftAlertController(nibName: "LiftAlertController", bundle: nil)
        ctrl.messageText = message
        ctrl.setBackgroundImage("Error")
        return ctrl
    }
    
    override func viewDidLoad() {
        self.message.text = messageText
    }
    
    func present(ovc: UIViewController) {
        ovc.presentViewController(self, animated: true, completion: nil)
    }
    
    @IBAction
    func close(sender: UIButton) {
        dismissViewControllerAnimated(true, completion: nil)
    }
    
    @IBOutlet
    var message: UILabel!
    
    
}
