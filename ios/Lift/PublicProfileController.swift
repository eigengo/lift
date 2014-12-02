import Foundation

class PublicProfileController : UIViewController {
    @IBOutlet
    var firstName: UITextField!
    @IBOutlet
    var lastName: UITextField!
    @IBOutlet
    var age: UITextField!
    @IBOutlet
    var weight: UITextField!
    
    @IBAction
    func save() {
        let publicProfile = User.PublicProfile(firstName: firstName.text,
            lastName: lastName.text,
            weight: weight.text.toInt(),
            age: age.text.toInt())
        
        LiftServer.sharedInstance.setPublicProfile(CurrentLiftUser.userId!, publicProfile: publicProfile) {
            $0.cata(LiftAlertController.showError("user_publicprofile_set_failed", parent: self), { _ in })
        }
    }

    private func showProfile(publicProfile: User.PublicProfile?) {
        if publicProfile != nil {
            firstName.text = publicProfile!.firstName
            lastName.text = publicProfile!.lastName
            if publicProfile!.age != nil {
                age.text = String(publicProfile!.age!)
            }
            if publicProfile!.weight != nil {
                weight.text = String(publicProfile!.weight!)
            }
        }
    }
    
    override func viewDidLoad() {
        LiftServer.sharedInstance.getPublicProfile(CurrentLiftUser.userId!) {
            $0.cata(LiftAlertController.showError("user_publicprofile_get_failed", parent: self), self.showProfile)
        }
    }
    
}