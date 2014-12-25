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
class ProfileController : UIViewController, UITableViewDataSource, UITableViewDelegate, PropertyTableViewCellDelegate {
    @IBOutlet
    var tableView: UITableView!
    @IBOutlet
    var saveButton: UIBarItem!
    
    // the user's profile
    private var profile: User.Profile = User.Profile.empty()
    
    // MARK: UITableViewDataSource implementation
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 3 
    }

    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return 2 // background and profile
        case 1: return 4 // four user properties
        case 2: return 1 // Pebble only for the moment
        
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 80
        case 1: return 40
        case 2: return 80
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        switch (indexPath.section, indexPath.row) {
        case (0, _): return tableView.dequeueReusableCellWithIdentifier("image") as UITableViewCell
        case (1, 0): return dequeueReusablePropertyTableViewCell(tableView, property: "firstName", delegate: self)
        case (1, 1): return dequeueReusablePropertyTableViewCell(tableView, property: "lastName", delegate: self)
        case (1, 2): return dequeueReusablePropertyTableViewCell(tableView, property: "age", delegate: self)
        case (1, 3): return dequeueReusablePropertyTableViewCell(tableView, property: "weight", delegate: self)
        case (2, _): return dequeueReusableDeviceTableViewCell(tableView)
        // cannot happen
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "ProfileController.pictures".localized()
        case 1: return "ProfileController.profile".localized()
        case 2: return "ProfileController.devices".localized()
            
        default: fatalError("Match error")
        }
    }
    
    
    
    // MARK: UITableViewDelegate implementation
    
    // MARK: PropertyTableViewCellDelegate implementation
    func propertyTableViewCellGetValueForProperty(property: String) -> String? {
        switch property {
        case "firstName": return profile.firstName
        case "lastName": return profile.lastName
        case "age": if let x = profile.age { return String(x) } else { return nil }
        case "weight": if let x = profile.weight { return String(x) } else { return nil }
        default: fatalError("Match error")
        }
    }
    
    func propertyTableViewCellValueValidate(value: String, property: String) -> String? {
        return nil
    }
    
    func propertyTableViewCellValueChanged(value: String, property: String) {
        saveButton.enabled = true
        switch property {
        case "firstName": profile.firstName = value
        case "lastName": profile.lastName = value
        case "age": profile.age = value.toInt()
        case "weight": profile.weight = value.toInt()
        default: fatalError("Match error")
        }
    }
    
    func propertyTableViewCellGetTitleForProperty(property: String) -> String {
        return "Profile.\(property)".localized()
    }
    
    // MARK: main
    
    @IBAction
    func save() {
        self.view.endEditing(true)
//        let publicProfile = User.Profile(firstName: firstName.text,
//            lastName: lastName.text,
//            weight: weight.text.toInt(),
//            age: age.text.toInt())
//        
//        LiftServer.sharedInstance.userSetPublicProfile(CurrentLiftUser.userId!, publicProfile: publicProfile) {
//            $0.cata(LiftAlertController.showError("user_publicprofile_set_failed", parent: self), { _ in })
//        }
    }

    private func showProfile(profile: User.Profile?) {
        if let x = profile { self.profile = x }
        tableView.reloadData()
    }
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.userGetProfile(CurrentLiftUser.userId!) {
            $0.cata(LiftAlertController.showError("user_publicprofile_get_failed", parent: self), self.showProfile)
        }
    }
    
}