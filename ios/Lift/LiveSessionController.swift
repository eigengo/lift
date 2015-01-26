import UIKit

class LiveSessionController: UITableViewController, UITableViewDelegate, UITableViewDataSource, ExerciseSessionSettable,
    SensorDataDelegate, DeviceDelegate {
    private let showSessionDetails = LiftUserDefaults.showSessionDetails
    private var connected: ConnectedDevices?
    private var exampleExercises: [Exercise.Exercise] = []
    private var timer: NSTimer?
    private var startTime: NSDate?
    private var session: ExerciseSession?
    @IBOutlet var stopSessionButton: UIBarButtonItem!

    // MARK: main
    override func viewWillDisappear(animated: Bool) {
        if let x = timer { x.invalidate() }
        navigationItem.prompt = nil
    }
    
    @IBAction
    func stopSession() {
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Really?".localized()
            stopSessionButton.tag = 3
        } else {
            end()
        }
    }
    
    func end() {
        if let x = session {
            x.end(const(()))
            self.session = nil
        } else {
            NSLog("[WARN] LiveSessionController.end() with sessionId == nil")
        }
    
        connected?.stop()
        UIApplication.sharedApplication().idleTimerDisabled = false
        if let x = navigationController {
            x.popToRootViewControllerAnimated(true)
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
        self.session = session
        connected = ConnectedDevices(deviceDelegate: self, sensorDataDelegate: self)
        connected!.start()

        session.getClassificationExamples { $0.getOrUnit { x in
                self.exampleExercises = x
                self.tableView.reloadData()
            }
        }
        UIApplication.sharedApplication().idleTimerDisabled = true
    }
    
    // MARK: UITableViewDataSource
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 3 // section 1: device & session, section 2: exercise log
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: if let x = connected { return x.deviceCount } else { return 0 }
        case 1: if let x = connected { return x.statsCount } else { return 0 }
        case 2: return exampleExercises.count
        default: return 0
        }
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        switch (indexPath.section, indexPath.row) {
        // section 1: device
        case (0, let x):
            let deviceInfo = connected!.deviceInfo(x)!
            return tableView.dequeueReusableDeviceTableViewCell(deviceInfo, deviceInfoDetail: nil, delegate: nil)
        case (1, let x):
            // TODO: iterate over all values, accelerometer now acceptable
            let (key, stats) = connected!.sessionStats(x)
            let cell = tableView.dequeueReusableCellWithIdentifier("sensor") as UITableViewCell
            cell.textLabel!.text = key.location.localized() + " " + key.sensorKind.localized()
            cell.detailTextLabel!.text = "LiveSessionController.sessionStatsDetail".localized(stats.bytes, stats.packets)
            return cell
        // section 2: exercise log
        case (2, let x):
            let cell = tableView.dequeueReusableCellWithIdentifier("exercise") as UITableViewCell
            cell.textLabel!.text = exampleExercises[x].name
            cell.selectionStyle = UITableViewCellSelectionStyle.None
            cell.accessoryType = UITableViewCellAccessoryType.None
            return cell
        default: return UITableViewCell()
        }
    }
    
    override func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "LiveSessionController.devices".localized()
        case 1: return "LiveSessionController.sensors".localized()
        case 2: return "LiveSessionController.exercise".localized()
        default: return ""
        }
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if indexPath.section == 1 {
            if let selectedCell = tableView.cellForRowAtIndexPath(indexPath) {
                switch selectedCell.accessoryType {
                case UITableViewCellAccessoryType.None:
                    for i in 0...(tableView.numberOfRowsInSection(1) - 1) {
                        let indexPath = NSIndexPath(forRow: i, inSection: 1)
                        if (tableView.cellForRowAtIndexPath(indexPath)!.accessoryType == UITableViewCellAccessoryType.Checkmark) {
                            tableView.cellForRowAtIndexPath(indexPath)!.accessoryType = UITableViewCellAccessoryType.None
                            session?.endExplicitClassification()
                        }
                    }
                    selectedCell.accessoryType = UITableViewCellAccessoryType.Checkmark
                    session?.startExplicitClassification(exampleExercises[indexPath.row])
                case UITableViewCellAccessoryType.Checkmark:
                    selectedCell.accessoryType = UITableViewCellAccessoryType.None
                    session?.endExplicitClassification()
                default: return
                }
            }
        }
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch (indexPath.section, indexPath.row) {
        case (0, 0): return 60
        default: return 40
        }
    }

    // MARK: SensorDataDelegate
    
    func sensorDataReceived(deviceSession: DeviceSession, data: NSData) {
        if let x = session {
            // TODO: Implement me
            //let mp = MutableMultiPacket().append(SensorDataSourceLocation.Wrist, data: data)
            //x.submitData(mp, const(()))

            if UIApplication.sharedApplication().applicationState != UIApplicationState.Background {
                tableView.reloadSections(NSIndexSet(index: 1), withRowAnimation: UITableViewRowAnimation.None)
            }
        } else {
            RKDropdownAlert.title("Internal inconsistency", message: "AD received, but no sessionId.", backgroundColor: UIColor.orangeColor(), textColor: UIColor.blackColor(), time: 3)
        }
    }
    
    func sensorDataEnded(deviceSession: DeviceSession) {
        end()
    }
    
    // MARK: DeviceDelegate
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo) {
        tableView.reloadData()
    }
    
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail) {
        tableView.reloadData()
    }
    
    func deviceAppLaunched(deviceId: DeviceId) {
        tableView.reloadData()
    }
    
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError) {
        tableView.reloadData()
    }
    
    func deviceDidNotConnect(error: NSError) {
        tableView.reloadData()
    }
    
    func deviceDisconnected(deviceId: DeviceId) {
        tableView.reloadData()
    }
    
}
