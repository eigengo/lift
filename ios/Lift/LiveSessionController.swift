import UIKit

class LiveSessionController: UITableViewController, UITableViewDelegate, UITableViewDataSource, ExerciseSessionSettable,
    AccelerometerDelegate, DeviceDelegate {
    private let showSessionDetails = LiftUserDefaults.showSessionDetails
    private var deviceInfo: DeviceInfo?
    private var deviceInfoDetail: DeviceInfo.Detail?
    private var deviceSession: DeviceSession?
    private var exampleExercises: [Exercise.Exercise] = []
    private var timer: NSTimer?
    private var startTime: NSDate?
    private var sessionId: NSUUID?
    private var device: ConnectedDevice?
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
        if let x = sessionId {
            LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: x) { x in
                NSLog("[INFO] LiveSessionController.end() session ended")
                self.sessionId = nil
            }
        } else {
            NSLog("[WARN] LiveSessionController.end() with sessionId == nil")
        }
    
        UIApplication.sharedApplication().idleTimerDisabled = false
        device?.stop()
        device = nil
        deviceSession = nil
        deviceInfo = nil
        deviceInfoDetail = nil
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
        sessionId = session.id
        device = PebbleConnectedDevice(deviceDelegate: self, deviceDataDelegates: DeviceDataDelegates(accelerometerDelegate: self))
        device!.start()
        LiftServer.sharedInstance.exerciseSessionGetClassificationExamples(CurrentLiftUser.userId!, sessionId: session.id) {
            $0.cata(const(()), { x in
                self.exampleExercises = x
                self.tableView.reloadData()
            })
        }
        UIApplication.sharedApplication().idleTimerDisabled = true
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
        case 1: return exampleExercises.count
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
        case (1, let x):
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
        case 0: return "LiveSessionController.section.deviceAndSession".localized()
        case 1: return "LiveSessionController.section.exercise".localized()
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
                        }
                    }
                    selectedCell.accessoryType = UITableViewCellAccessoryType.Checkmark
                    ResultContext.run { ctx in
                        LiftServer.sharedInstance.exerciseSessionStartExplicitClassification(CurrentLiftUser.userId!, sessionId: self.sessionId!, exercise: self.exampleExercises[indexPath.row], f: ctx.unit())
                    }
                case UITableViewCellAccessoryType.Checkmark:
                    selectedCell.accessoryType = UITableViewCellAccessoryType.None
                    ResultContext.run { ctx in
                        LiftServer.sharedInstance.exerciseSessionEndExplicitClassification(CurrentLiftUser.userId!, sessionId: self.sessionId!, f: ctx.unit())
                    }
                default: return
                }
            }
        }
    }
    
    override func tableView(tableView: UITableView, didDeselectRowAtIndexPath indexPath: NSIndexPath) {
        if let selectedCell = tableView.cellForRowAtIndexPath(indexPath) {
            switch selectedCell.accessoryType {
                //If it was still checked, send delete request before unchecking
            case UITableViewCellAccessoryType.Checkmark:
                selectedCell.accessoryType = UITableViewCellAccessoryType.None
                ResultContext.run{ ctx in
                    LiftServer.sharedInstance.exerciseSessionEndExplicitClassification(CurrentLiftUser.userId!, sessionId: self.sessionId!, f: ctx.unit())
                }
            default: return
            }
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
        if let x = sessionId {
            self.deviceSession = deviceSession
            let mp = MutableMultiPacket().append(SensorDataSourceLocation.Wrist, data: data)
            LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: x, data: mp) {
                $0.cata({ _ in /* TODO: offline mode save */ }, const(()))
            }
            if UIApplication.sharedApplication().applicationState != UIApplicationState.Background {
                tableView.reloadData()
            }
        } else {
            RKDropdownAlert.title("Internal inconsistency", message: "AD received, but no sessionId.", backgroundColor: UIColor.orangeColor(), textColor: UIColor.blackColor(), time: 3)
        }
    }
    
    func accelerometerDataEnded(deviceSession: DeviceSession) {
        end()
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
        tableView.reloadData()
    }
    
    func deviceDidNotConnect(error: NSError) {
        NSLog("deviceDidNotConnect %@", error)
        tableView.reloadData()
    }
    
    func deviceDisconnected(deviceId: DeviceId) {
        NSLog("deviceDisconnected %@", deviceId)
        tableView.reloadData()
    }
    
}
