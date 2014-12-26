import Foundation

/*
 * Handles public profile, which includes public picture, name & other details and devices
 */
class ProfileController : UIViewController, UITableViewDataSource, UITableViewDelegate, PropertyTableViewCellDelegate {
    @IBOutlet
    var tableView: UITableView!
    @IBOutlet
    var saveButton: UIBarItem!
    
    // the devices
    private var devices: [DeviceInfo] = []
    
    // the user's profile
    private var profile: User.Profile = User.Profile.empty()
    
    // MARK: UITableViewDataSource implementation
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 3 
    }

    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return 1 // profile picture
        case 1: return 4 // four user properties
        case 2: return 1 // Pebble,
        
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 60
        case 1: return 40
        case 2: return 60
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
    private func optIntToString(i: Int?) -> String? {
        if let x = i { return String(x) } else { return nil }
    }
    
    func propertyTableViewCellGetProperty(property: String) -> PropertyDescriptor {
        switch property {
        case "firstName": return PropertyDescriptor(title: "Profile.firstName".localized(), value: profile.firstName)
        case "lastName": return PropertyDescriptor(title: "Profile.lastName".localized(), value: profile.lastName)
        case "age": return PropertyDescriptor(title: "Profile.age".localized(), type: .Integer(0, 150)).fold(profile.age, optIntToString)
        case "weight": return PropertyDescriptor(title: "Profile.weight".localized(), type: .Integer(0, 400)).fold(profile.weight, optIntToString)
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
    
    // MARK: main
    
    @IBAction
    func save() {
        self.view.endEditing(true)
        LiftServer.sharedInstance.userSetProfile(CurrentLiftUser.userId!, profile: profile) {
            $0.cata(LiftAlertController.showError("user_publicprofile_set_failed", parent: self), { _ in })
        }
        saveButton.enabled = false
    }

    private func showProfile(profile: User.Profile?) {
        if let x = profile { self.profile = x }
        saveButton.enabled = false
        tableView.reloadData()
    }
    
    private func peekDevices() {
        Devices.peek { $0.either({ (error, type) in },
            onR: { info in self.devices += [info]; self.tableView.reloadData() } ) }
    }
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.userGetProfile(CurrentLiftUser.userId!) {
            $0.cata(LiftAlertController.showError("user_publicprofile_get_failed", parent: self), self.showProfile)
        }
        peekDevices()
    }
    
}