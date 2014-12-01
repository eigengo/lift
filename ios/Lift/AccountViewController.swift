import Foundation

class AccountViewController : UIViewController {
    @IBOutlet var username: UITextField!
    @IBOutlet var password: UITextField!
    
    private func showError(error: NSError) {
        LiftAlertController.error("Could not log in", error: error).present(self)
    }
    
    private func showAccount(user: User) {
        performSegueWithIdentifier("account_account", sender: nil)        
    }
    
    @IBAction
    func login(sender: UIButton) {
        LiftServer.sharedInstance.login(username.text, password: password.text).cata(showError, showAccount)
    }
    
    @IBAction
    func register(sender: UIButton) {
        LiftServer.sharedInstance.register(username.text, password: password.text) { r in r.cata(self.showError, self.showAccount) }
    }
}