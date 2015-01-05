import UIKit

class LiveSessionController: UITableViewController, UITableViewDelegate, UITableViewDataSource, ExerciseSessionSettable,
    AccelerometerDelegate, DeviceDelegate {
    private let showSessionDetails = LiftUserDefaults.showSessionDetails
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
            stopSessionButton.title = "Really?".localized()
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
        navigationItem.prompt = "LiveSessionController.elapsed".localized(minutes, seconds)
        stopSessionButton.tag -= 1
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Stop".localized()
        }
    }

    // MARK: ExerciseSessionSettable
    func setExerciseSession(session: ExerciseSession) {
        PebbleConnectedDevice(deviceDelegate: self, deviceDataDelegates: DeviceDataDelegates(accelerometerDelegate: self)).start()
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
            return tableView.dequeueReusableDeviceTableViewCell(deviceInfo, deviceInfoDetail: deviceInfoDetail, delegate: nil)
        case (0, let x):
            let index = x - 1
            // TODO: iterate over all values, accelerometer now acceptable
            let (key, stats) = deviceSession!.sessionStats()[index]
            let cell = tableView.dequeueReusableCellWithIdentifier("session") as UITableViewCell
            cell.textLabel!.text = key.localized()
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
        case (0, 0): return 60
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
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo) {
        self.deviceInfo = deviceInfo
        tableView.reloadData()
    }
    
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail) {
        deviceInfoDetail = detail
        tableView.reloadData()
    }
    
    func deviceAppLaunched(deviceId: DeviceId) {
        tableView.reloadData()
    }
    
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError) {
        NSLog("deviceAppLaunchFailed %@ -> %@", deviceId, error)
        deviceInfo = nil
        tableView.reloadData()
    }
    
    func deviceDidNotConnect(error: NSError) {
        NSLog("deviceDidNotConnect %@", error)
        deviceInfo = nil
        tableView.reloadData()
    }
    
    func deviceDisconnected(deviceId: DeviceId) {
        NSLog("deviceDisconnected %@", deviceId)
        deviceInfo = nil
        deviceInfoDetail = nil
        deviceSession = nil
        tableView.reloadData()
    }
    
}
