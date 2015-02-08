import Foundation

class DeviceSettingsController : UITableViewController {
    var deviceInfo: DeviceInfo?
    let deviceLocations = [DeviceInfo.Location.Wrist, DeviceInfo.Location.Waist, DeviceInfo.Location.Chest, DeviceInfo.Location.Foot, DeviceInfo.Location.Any]
    
    func setDeviceInfo(deviceInfo: DeviceInfo) {
        self.deviceInfo = deviceInfo
        tableView.reloadData()
    }
    
    // MARK: UITableViewDataSource implementation
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return deviceInfo != nil ? 1 : 0
        case 1: return deviceLocations.count
        default: fatalError("Match error")
        }
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 60   // profile picture
        case 1: return 40    // followers
            
        default: fatalError("Match error")
        }
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2
    }
 
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        switch (indexPath.section, indexPath.row) {
        case (0, 0): return tableView.dequeueReusableDeviceTableViewCell(deviceInfo!, deviceInfoDetail: nil)
        case (1, let x):
            let dl = deviceLocations[x]
            let cell = tableView.dequeueReusableCellWithIdentifier("location") as UITableViewCell
            if dl == LiftUserDefaults.getLocation(deviceInfo: deviceInfo!) {
                cell.accessoryType = UITableViewCellAccessoryType.Checkmark
            } else {
                cell.accessoryType = UITableViewCellAccessoryType.None
            }
            cell.textLabel!.text = dl.localized()
            cell.detailTextLabel!.text = dl.localisedDescription()
            return cell
        default: fatalError("Match error")
        }
    }
    
    // MARK: UITableViewDelegate
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if indexPath.section == 1 {
            let dl = deviceLocations[indexPath.row]
            tableView.deselectRowAtIndexPath(indexPath, animated: false)
            LiftUserDefaults.setLocation(deviceInfo: deviceInfo!, location: dl)
            tableView.reloadData()
        }
    }
    
    // MARK: main
    @IBAction
    func save() {
        navigationController?.popViewControllerAnimated(true)
    }
    
    @IBAction
    func cancel() {
        navigationController?.popViewControllerAnimated(true)
    }

}
