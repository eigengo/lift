import Foundation
import MobileCoreServices

/**
 * Profile cell shows the user's picture
 */
class ProfileImageTableViewCell : UITableViewCell {
    @IBOutlet
    var profileImageView: UIImageView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        if profileImageView == nil { return }
            
        profileImageView.layer.cornerRadius = profileImageView.frame.width / 2
        profileImageView.layer.borderColor = tintColor.CGColor
        profileImageView.layer.borderWidth = 2
        profileImageView.clipsToBounds = true
    }
}

/*
 * Handles public profile, which includes public picture, name & other details and devices
 */
class PublicProfileController : UIViewController, UITableViewDataSource, UITableViewDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate, PropertyTableViewCellDelegate, DeviceTableViewCellDelegate {
    @IBOutlet
    var tableView: UITableView!
    @IBOutlet
    var saveButton: UIBarItem!
    
    // the devices
    private var deviceInfos: [DeviceInfo] = []
    
    // the user's image
    private var profileImage: NSData?
    
    // the user's profile
    private var profile: User.PublicProfile = User.PublicProfile.empty()
    
    // followers
    private var followersCount = 0
    private var followingCount = 0
    
    // MARK: UITableViewDataSource implementation
    
    // We have four sections
    // * profile picture
    // * public profile
    // * following
    // * devices
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 4
    }

    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return 1                 // profile picture
        case 1: return 4                 // four user properties
        case 2: return 2                 // followers
        case 3: return deviceInfos.count // devices
        
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 100   // profile picture
        case 1: return 40   // four user properties
        case 2: return 40   // followers
        case 3: return 60   // devices
            
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        switch (indexPath.section, indexPath.row) {
        case (0, _):
            let cell = tableView.dequeueReusableCellWithIdentifier("image") as ProfileImageTableViewCell
            if let x = profileImage { cell.profileImageView.image = UIImage(data: x) }
            return cell
        case (1, 0): return tableView.dequeueReusablePropertyTableViewCell("firstName", delegate: self)
        case (1, 1): return tableView.dequeueReusablePropertyTableViewCell("lastName", delegate: self)
        case (1, 2): return tableView.dequeueReusablePropertyTableViewCell("age", delegate: self)
        case (1, 3): return tableView.dequeueReusablePropertyTableViewCell("weight", delegate: self)
        case (2, 0):
            let cell = tableView.dequeueReusableCellWithIdentifier("following") as UITableViewCell
            cell.textLabel!.text = "PublicProfileController.followingText".localized()
            cell.detailTextLabel!.text = "PublicProfileController.followingDetail".localized(followingCount)
            return cell
        case (2, 1):
            let cell = tableView.dequeueReusableCellWithIdentifier("following") as UITableViewCell
            cell.textLabel!.text = "PublicProfileController.followersText".localized()
            cell.detailTextLabel!.text = "PublicProfileController.followersDetail".localized(followersCount)
            return cell
        case (3, let x): return tableView.dequeueReusableDeviceTableViewCell(deviceInfos[x], deviceInfoDetail: nil, delegate: self)
        // cannot happen
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "PublicProfileController.pictures".localized()
        case 1: return "PublicProfileController.profile".localized()
        case 2: return "PublicProfileController.social".localized()
        case 3: return "PublicProfileController.devices".localized()
            
        default: fatalError("Match error")
        }
    }
    
    // MARK: UITableViewDelegate implementation
    
    private func setProfilePicture() {
        let imagePicker = UIImagePickerController()
        imagePicker.delegate = self
        //imagePicker.sourceType = UIImagePickerControllerSourceType.Camera
        imagePicker.mediaTypes = [kUTTypeImage]
        imagePicker.allowsEditing = true
        presentViewController(imagePicker, animated: true, completion: nil)
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        switch (indexPath.section, indexPath.row) {
        case (0, 0):
            tableView.deselectRowAtIndexPath(indexPath, animated: true)
            setProfilePicture()
        default: return // noop
        }
    }
    
    // MARK: UIImagePickerControllerDelegate implementation
    
    func imagePickerController(picker: UIImagePickerController!, didFinishPickingImage image: UIImage!, editingInfo: [NSObject : AnyObject]!) {
        let w: CGFloat = 200.0
        let h: CGFloat = 200.0
        if image.size.width > w || image.size.height > h {
            let sx = w / CGFloat(image.size.width)
            let sy = h / CGFloat(image.size.height)
            let s = min(sx, sy)
            let newSize = CGSizeMake(image.size.width * s, image.size.height * s)
            UIGraphicsBeginImageContext(newSize)
            image.drawInRect(CGRectMake(0, 0, newSize.width, newSize.height))
            let scaledImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            profileImage = UIImageJPEGRepresentation(scaledImage, 0.6)
        } else {
            profileImage = UIImageJPEGRepresentation(image, 0.6)
        }
        
        LiftServer.sharedInstance.userSetProfileImage(CurrentLiftUser.userId!, image: profileImage!, const(()))
        picker.dismissViewControllerAnimated(true, completion: nil)
    }
    
    func imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion: nil)
    }
    
    // MARK: DeviceTableViewCellDelegate implementaion
    
    func deviceTableViewCellAccessorySwitchValue(deviceId: DeviceId) -> Bool {
        return true
    }
    
    func deviceTableViewCellAccessorySwitchValueChanged(deviceId: DeviceId, value: Bool) -> Bool {
        return value
    }
    
    func deviceTableViewCellShowAccessorySwitch(deviceId: DeviceId) -> Bool {
        return true
    }
    
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
        ResultContext.run { ctx in
            LiftServer.sharedInstance.userSetPublicProfile(CurrentLiftUser.userId!, profile: self.profile, ctx.unit())
        }
        navigationController?.popViewControllerAnimated(true)
    }
    
    @IBAction
    func cancel() {
        navigationController?.popViewControllerAnimated(true)
    }

    private func showProfile(profile: User.PublicProfile?) {
        if let x = profile { self.profile = x }
        saveButton.enabled = false
        tableView.reloadData()
    }
    
    private func peekDevices() {
        self.deviceInfos = []
        Devices.peek { x in self.deviceInfos += [x]; self.tableView.reloadData() }
    }
        
    override func viewDidAppear(animated: Bool) {
        ResultContext.run { ctx in
            LiftServer.sharedInstance.userGetPublicProfile(CurrentLiftUser.userId!, ctx.apply(self.showProfile))
            LiftServer.sharedInstance.userGetProfileImage(CurrentLiftUser.userId!, ctx.apply { image in
                self.profileImage = image
                self.tableView.reloadData()
            })
        }
        peekDevices()
    }
    
}