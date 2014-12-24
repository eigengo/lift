import UIKit

class DeviceTableViewCell : UITableViewCell {
    @IBOutlet var name: UILabel!
    @IBOutlet var detail: UILabel!
    
    func setDeviceInfo(deviceInfo: DeviceInfo?, deviceInfoDetail: DeviceInfo.Detail?) {
        if let di = deviceInfo {
            name.text = di.name
            if let did = deviceInfoDetail {
                detail.text = "DeviceTableViewCell.deviceInfoWithDetail".localized(di.serialNumber, did.address)
            } else {
                detail.text = "DeviceTableViewCell.deviceInfo".localized(di.serialNumber)
            }
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
    private var deviceInfoDetail: DeviceInfo.Detail?
    private var deviceSession: DeviceSession?
    private var timer: NSTimer?
    private var startTime: NSDate?
    private var sessionId: NSUUID?
    @IBOutlet var stopSessionButton: UIBarButtonItem!

    // MARK: main
    override func viewWillDisappear(animated: Bool) {
        timer!.invalidate()
        navigationItem.prompt = nil
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: sessionId!) { _ in }
    }
    
    @IBAction
    func stopSession() {
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Really?"
            stopSessionButton.tag = 3
        } else {
            navigationController!.popToRootViewControllerAnimated(true)
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        startTime = NSDate()
        timer = NSTimer.scheduledTimerWithTimeInterval(1, target: self, selector: "tick", userInfo: nil, repeats: true)
    }
    
    func tick() {
        let elapsed = Int(NSDate().timeIntervalSinceDate(startTime!))
        let minutes: Int = elapsed / 60
        let seconds: Int = elapsed - minutes * 60
        navigationItem.prompt = NSString(format: "Elapsed %d:%02d", minutes, seconds)
        stopSessionButton.tag -= 1
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Stop"
        }
    }

    // MARK: ExerciseSessionSettable
    func setExerciseSession(session: ExerciseSession) {
        PebbleDevice(deviceDelegate: self, deviceDataDelegates: DeviceDataDelegates(accelerometerDelegate: self))
        sessionId = session.id
    }
    
    // MARK: UITableViewDataSource
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2 // section 1: device & session, section 2: exercise log
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        // section 1: device & session
        case 0:
            if deviceSession != nil {
                // device connected
                return showSessionDetails ? 1 + deviceSession!.sessionStats().count : 1
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
            cell.setDeviceInfo(deviceInfo, deviceInfoDetail: deviceInfoDetail)
            return cell
        case (0, let x):
            let index = x - 1
            // TODO: iterate over all values, accelerometer now acceptable
            let stats = deviceSession!.sessionStats()["accelerometer"]!
            let cell = tableView.dequeueReusableCellWithIdentifier("session") as UITableViewCell
            cell.textLabel!.text = "LiveSessionController.sessionStatsTitle".localized("accelerometer")
            cell.detailTextLabel!.text = "LiveSessionController.sessionStatsDetail".localized(stats.bytes, stats.packets)
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
        switch (indexPath.section, indexPath.row) {
        case (0, 0): return 85
        default: return 40
        }
    }

    // MARK: AccelerometerReceiverDelegate
    
    func accelerometerDataReceived(deviceSession: DeviceSession, data: NSData) {
        self.deviceSession = deviceSession
        tableView.reloadData()
    }
    
    func accelerometerDataEnded(deviceSession: DeviceSession) {
        self.deviceSession = nil
        deviceInfo = nil
        deviceInfoDetail = nil
        navigationController!.popToRootViewControllerAnimated(true)
        tableView.reloadData()
    }
    
    // MARK: DeviceDelegate
    func deviceGotDeviceInfo(deviceId: NSUUID, deviceInfo: DeviceInfo) {
        self.deviceInfo = deviceInfo
        tableView.reloadData()
    }
    
    func deviceGotDeviceInfoDetail(deviceId: NSUUID, detail: DeviceInfo.Detail) {
        self.deviceInfoDetail = detail
        tableView.reloadData()
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
        self.deviceInfoDetail = nil
        self.deviceSession = nil
        tableView.reloadData()
    }
    
}
