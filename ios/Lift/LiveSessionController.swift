import UIKit

class DeviceTableViewCell : UITableViewCell {
    @IBOutlet var name: UILabel!
    @IBOutlet var detail: UILabel!
    
    func setDeviceInfo(deviceInfo: DeviceInfo?) {
        if let di = deviceInfo {
            name.text = di.name
            detail.text = String(format: "Serial %@", di.serialNumber)
        } else {
            name.text = "DeviceTableViewCell.noDevice".localized()
            detail.text = ""
        }
    }
    
    func setDeviceError(error: NSError) {
        name.text = String(format: "%@", error)
        detail.text = ""
    }
}

class LiveSessionController: UITableViewController, UITableViewDelegate, UITableViewDataSource, ExerciseSessionSettable,
    AccelerometerDelegate, DeviceDelegate {
    // TODO: Move to settings
    private let showSessionDetails = true
    private var deviceInfo: DeviceInfo?
    private var accelerometerSessionStats: [NSUUID : AccelerometerSessionStats] = [:]
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    func setExerciseSession(session: ExerciseSession) {
        PebbleDevice(deviceDelegate: self, deviceDataDelegates: DeviceDataDelegates(accelerometerDelegate: self))
        NSLog("Starting with %@", session)
    }
    
    // #pragma mark - UITableViewDataSource
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2 // section 1: device & session, section 2: exercise log
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        // section 1: device & session
        case 0:
            if deviceInfo != nil {
                // device connected
                return showSessionDetails ? 1 + accelerometerSessionStats.count : 1
            } else {
                // no device
                return 1
            }
        // section 2: exercise log
        case 1: return 10
        default: return 0
        }
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        switch (indexPath.section, indexPath.row) {
        // section 1: device
        case (0, 0):
            let cell = tableView.dequeueReusableCellWithIdentifier("device") as DeviceTableViewCell
            cell.setDeviceInfo(deviceInfo)
            return cell
        case (0, let x):
            let index = x - 1
            let cell = tableView.dequeueReusableCellWithIdentifier("session") as UITableViewCell
            cell.textLabel!.text = "LiveSessionController.sessionTitle".localized(index)
            cell.detailTextLabel!.text = "Received x B, y packets."
            return cell
        // section 2: exercise log
        case (1, _):
            let cell = tableView.dequeueReusableCellWithIdentifier("exercise") as UITableViewCell
            cell.textLabel!.text = "LiveSessionController.exercise".localized()
            return cell
        default: return UITableViewCell()
        }
    }
    
    override func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "LiveSessionController.section.deviceAndSession".localized()
        case 1: return "LiveSessionController.section.exercise".localized()
        default: return ""
        }
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 85
        case 1: return 40
        default: return 0
        }
    }

    // #pragma mark - AccelerometerReceiverDelegate
    
    func accelerometerReceiverReceived(deviceSession: NSUUID, data: NSData, stats: AccelerometerSessionStats) {
        accelerometerSessionStats[deviceSession] = stats
        tableView.reloadData()
    }
    
    func accelerometerReceiverEnded(deviceSession: NSUUID, stats: AccelerometerSessionStats?) {
        accelerometerSessionStats[deviceSession] = nil
        tableView.reloadData()
    }

    // #pragma mark - DeviceDelegate
    func deviceGotDeviceInfo(deviceId: NSUUID, deviceInfo: DeviceInfo) {
        self.deviceInfo = deviceInfo
        tableView.reloadData()
    }
    
    func deviceGotDeviceInfoDetail(deviceId: NSUUID, detail: DeviceInfo.Detail) {
//        self.deviceInfo = deviceInfo
//        tableView.reloadData()
    }
    
    func deviceAppLaunched(deviceId: NSUUID) {
        tableView.reloadData()
    }
    
    func deviceAppLaunchFailed(deviceId: NSUUID, error: NSError) {
        NSLog("deviceAppLaunchFailed %@ -> %@", deviceId, error)
        self.deviceInfo = nil
        tableView.reloadData()
    }
    
    func deviceDidNotConnect(error: NSError) {
        NSLog("deviceDidNotConnect %@", error)
        self.deviceInfo = nil
        tableView.reloadData()
    }
    
    func deviceDisconnected(deviceId: NSUUID) {
        NSLog("deviceDisconnected %@", deviceId)
        self.deviceInfo = nil
        self.accelerometerSessionStats = [:]
        tableView.reloadData()
    }
    
}
