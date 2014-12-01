import Foundation

class AccountViewController : UIViewController {
    @IBOutlet var username: UITextField!
    @IBOutlet var password: UITextField!
    
    private func showError(key: String) -> NSError -> Void {
        return { error in LiftAlertController.error(NSLocalizedString(key, comment: ""), error: error).present(self) }
    }
    
    private func showAccount(user: User) {
        let deviceToken = (UIApplication.sharedApplication().delegate! as AppDelegate).deviceToken
        if deviceToken != nil {
            LiftServer.sharedInstance.registerDeviceToken(user.id, deviceToken: deviceToken!)
        }
        performSegueWithIdentifier("account_account", sender: nil)
    }
    
    @IBAction
    func login(sender: UIButton) {
        LiftServer.sharedInstance.login(username.text, password: password.text) { $0.cata(self.showError("user_loginfailed"), self.showAccount) }
    }
    
    @IBAction
    func register(sender: UIButton) {
        LiftServer.sharedInstance.register(username.text, password: password.text) { $0.cata(self.showError("user_registerfailed"), self.showAccount) }
    }
}
