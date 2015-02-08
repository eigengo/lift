import UIKit

class LiveSessionController: UITableViewController, UITableViewDelegate, UITableViewDataSource, ExerciseSessionSettable,
    DeviceSessionDelegate, DeviceDelegate {
    private let showSessionDetails = LiftUserDefaults.showSessionDetails
    private var multi: MultiDeviceSession?
    private var exampleExercises: [Exercise.Exercise] = []
    private var timer: NSTimer?
    private var startTime: NSDate?
    private var exerciseSession: ExerciseSession?
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
        if let x = exerciseSession {
            x.end(const(()))
            self.exerciseSession = nil
        } else {
            NSLog("[WARN] LiveSessionController.end() with sessionId == nil")
        }
    
        multi?.stop()
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
        self.exerciseSession = session
        multi = MultiDeviceSession(deviceDelegate: self, deviceSessionDelegate: self)
        multi!.start()

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
        case 0: if let x = multi { return x.deviceInfoCount } else { return 0 }
        case 1: if let x = multi { return x.sessionStatsCount } else { return 0 }
        case 2: return exampleExercises.count
        default: return 0
        }
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        switch (indexPath.section, indexPath.row) {
        // section 1: device
        case (0, let x):
            let cdi = multi!.getDeviceInfo(x)
            return tableView.dequeueReusableDeviceTableViewCell(cdi.deviceInfo, deviceInfoDetail: cdi.deviceInfoDetail)
        // section 2: sensors
        case (1, let x):
            let (key, stats) = multi!.getSessionStats(x)
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

    /*
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if indexPath.section == 1 {
            if let selectedCell = tableView.cellForRowAtIndexPath(indexPath) {
                switch selectedCell.accessoryType {
                case UITableViewCellAccessoryType.None:
                    for i in 0...(tableView.numberOfRowsInSection(1) - 1) {
                        let indexPath = NSIndexPath(forRow: i, inSection: 1)
                        if (tableView.cellForRowAtIndexPath(indexPath)!.accessoryType == UITableViewCellAccessoryType.Checkmark) {
                            tableView.cellForRowAtIndexPath(indexPath)!.accessoryType = UITableViewCellAccessoryType.None
                            exerciseSession?.endExplicitClassification()
                        }
                    }
                    selectedCell.accessoryType = UITableViewCellAccessoryType.Checkmark
                    exerciseSession?.startExplicitClassification(exampleExercises[indexPath.row])
                case UITableViewCellAccessoryType.Checkmark:
                    selectedCell.accessoryType = UITableViewCellAccessoryType.None
                    exerciseSession?.endExplicitClassification()
                default: return
                }
            }
        }
    }
    */
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 60
        default: return 40
        }
    }

    // MARK: DeviceSessionDelegate
    func deviceSession(session: DeviceSession, finishedWarmingUp deviceId: DeviceId) {
        // ???
    }
    
    func deviceSession(session: DeviceSession, startedWarmingUp deviceId: DeviceId, expectedCompletionIn time: NSTimeInterval) {
        // ???
    }

    func deviceSession(session: DeviceSession, endedFrom deviceId: DeviceId) {
        end()
    }
    
    func deviceSession(session: DeviceSession, sensorDataNotReceivedFrom deviceId: DeviceId) {
        // ???
    }
    
    func deviceSession(session: DeviceSession, sensorDataReceivedFrom deviceId: DeviceId, atDeviceTime: CFAbsoluteTime, data: NSData) {
        if let x = exerciseSession {
            x.submitData(data, f: const(()))
            
            if UIApplication.sharedApplication().applicationState != UIApplicationState.Background {
                tableView.reloadSections(NSIndexSet(index: 1), withRowAnimation: UITableViewRowAnimation.None)
            }
        } else {
            RKDropdownAlert.title("Internal inconsistency", message: "AD received, but no sessionId.", backgroundColor: UIColor.orangeColor(), textColor: UIColor.blackColor(), time: 3)
        }
    }
    
    // MARK: DeviceDelegate
    func deviceGotDeviceInfo(deviceId: DeviceId, deviceInfo: DeviceInfo) {
        tableView.reloadSections(NSIndexSet(index: 0), withRowAnimation: UITableViewRowAnimation.Automatic)
    }
    
    func deviceGotDeviceInfoDetail(deviceId: DeviceId, detail: DeviceInfo.Detail) {
        tableView.reloadSections(NSIndexSet(index: 0), withRowAnimation: UITableViewRowAnimation.Automatic)
    }
    
    func deviceAppLaunched(deviceId: DeviceId) {
        //
    }
    
    func deviceAppLaunchFailed(deviceId: DeviceId, error: NSError) {
        //
    }
    
    func deviceDidNotConnect(error: NSError) {
        tableView.reloadSections(NSIndexSet(index: 0), withRowAnimation: UITableViewRowAnimation.Automatic)
    }
    
    func deviceDisconnected(deviceId: DeviceId) {
        tableView.reloadSections(NSIndexSet(index: 0), withRowAnimation: UITableViewRowAnimation.Automatic)
    }
    
}
