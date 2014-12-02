import UIKit

class LiftAlertController: UIViewController {
    var messageText: String?
    var imageNames: [String]?

    class func error(message: String, error: NSError) -> LiftAlertController {
        let ctrl = LiftAlertController(nibName: "LiftAlertController", bundle: nil)
        ctrl.messageText = message + " " + error.localizedDescription
        ctrl.imageNames = ["Error"]
        return ctrl
    }
    
    class func showError(key: String, parent: UIViewController) -> (NSError) -> Void {
        return { error in LiftAlertController.error(NSLocalizedString(key, comment: ""), error: error).present(parent) }
    }
    
    override func viewDidLayoutSubviews() {
        if (imageNames != nil) {
            setBackgroundImage(imageNames!)
        }
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
