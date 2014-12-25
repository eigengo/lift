import Foundation

class ProfileControllerImageTableViewCell : UITableViewCell {
    
}

class ProfileControllerTextTableViewCell : UITableViewCell {
    
}

class ProfileControllerDeviceTableViewCell : UITableViewCell {
    
}

/*
 * Handles public profile, which includes public picture, name & other details and devices
 */
class ProfileController : UIViewController, UITableViewDataSource, UITableViewDelegate {
    @IBOutlet
    var tableView: UITableView!
    @IBOutlet
    var saveButton: UIBarItem!
    
    // the user's profile
    private var profile: User.Profile?
    
    // MARK: UITableViewDataSource implementation
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return 2; // background and profile
        case 1: return 4; // four user properties
        case 2: return 1; // Pebble only for the moment
        
        default: return 0; // no cells here
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        switch (indexPath.section, indexPath.row) {
        case (0, _): return tableView.dequeueReusableCellWithIdentifier("image") as UITableViewCell
        case (1, _): return tableView.dequeueReusableCellWithIdentifier("text") as UITableViewCell
        case (2, _): return dequeueReusableDeviceTableViewCell(tableView)
        // cannot happen
        default: return tableView.dequeueReusableCellWithIdentifier("text") as UITableViewCell
        }
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "ProfileController.pictures".localized()
        case 1: return "ProfileController.profile".localized()
        case 2: return "ProfileController.devices".localized()
            
        default: return ""  // no text here
        }
    }
    
    // MARK: UITableViewDelegate implementation
    
    // MARK: main
    
    @IBAction
    func save() {
        self.view.endEditing(true)
        let publicProfile = User.Profile(firstName: firstName.text,
            lastName: lastName.text,
            weight: weight.text.toInt(),
            age: age.text.toInt())
        
        LiftServer.sharedInstance.userSetPublicProfile(CurrentLiftUser.userId!, publicProfile: publicProfile) {
            $0.cata(LiftAlertController.showError("user_publicprofile_set_failed", parent: self), { _ in })
        }
    }

    private func showProfile(profile: User.Profile?) {
        self.profile = profile
        tableView.reloadData()
    }
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.userGetProfile(CurrentLiftUser.userId!) {
            $0.cata(LiftAlertController.showError("user_publicprofile_get_failed", parent: self), self.showProfile)
        }
    }
    
}