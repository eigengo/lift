import Foundation

class AccountViewController : UIViewController {
    @IBOutlet var username: UITextField!
    @IBOutlet var password: UITextField!
    
    private func showAccount(user: User) {
        let deviceToken = (UIApplication.sharedApplication().delegate! as AppDelegate).deviceToken
        if deviceToken != nil {
            LiftServer.sharedInstance.registerDeviceToken(user.id, deviceToken: deviceToken!)
        }
        CurrentLiftUser.userId = user.id
        performSegueWithIdentifier("account_account", sender: nil)
    }
    
    @IBAction
    func login(sender: UIButton) {
        LiftServer.sharedInstance.login(username.text, password: password.text) {
            $0.cata(LiftAlertController.showError("user_loginfailed", parent: self), self.showAccount)
        }
    }
    
    @IBAction
    func register(sender: UIButton) {
        LiftServer.sharedInstance.register(username.text, password: password.text) {
            $0.cata(LiftAlertController.showError("user_loginfailed", parent: self), self.showAccount)
        }
    }
}
