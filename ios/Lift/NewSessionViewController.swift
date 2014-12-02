import Foundation

class NewSessionViewController : UIViewController {
    
    override func viewDidLoad() {
        setBackgroundImage(["Start1", "Start2"])
    }
 
    @IBAction
    func startSession() {
        performSegueWithIdentifier("newsession_active", sender: nil)
    }
            
}