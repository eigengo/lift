import Foundation

class AccountViewController : UIViewController {
    @IBOutlet var username: UITextField!
    @IBOutlet var password: UITextField!
    
    private func showAccount(user: User) {
        let deviceToken = (UIApplication.sharedApplication().delegate! as AppDelegate).deviceToken
        if deviceToken != nil {
            LiftServer.sharedInstance.userRegisterDeviceToken(user.id, deviceToken: deviceToken!)
        }
        CurrentLiftUser.userId = user.id
        performSegueWithIdentifier("main", sender: nil)
    }
    
    @IBAction
    func login(sender: UIButton) {
        view.endEditing(true)
        ResultContext.run { ctx in
            LiftServer.sharedInstance.userLogin(self.username.text, password: self.password.text, ctx.apply(self.showAccount))
        }
    }
    
    @IBAction
    func register(sender: UIButton) {
        view.endEditing(true)
        ResultContext.run { ctx in
            LiftServer.sharedInstance.userRegister(self.username.text, password: self.password.text, ctx.apply(self.showAccount))
        }
    }
}
