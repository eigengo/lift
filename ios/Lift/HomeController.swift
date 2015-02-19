import Foundation

class SessionTableViewCell : UITableViewCell, JBBarChartViewDataSource, JBBarChartViewDelegate {
    @IBOutlet var titleLabel: UILabel!
    @IBOutlet var detailLabel: UILabel!
    @IBOutlet var chartView: UIView!
    private var intensityChart: JBBarChartView?
    private var sessionSummary: Exercise.SessionSummary?

    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override func awakeFromNib() {
        self.intensityChart = JBBarChartView()
        self.intensityChart!.dataSource = self
        self.intensityChart!.delegate = self
        self.intensityChart!.minimumValue = 0
        self.intensityChart!.maximumValue = 1
        self.intensityChart!.userInteractionEnabled = false
        self.intensityChart!.showsVerticalSelection = false
        self.intensityChart!.frame = CGRectMake(0, 0, 50, 70)
        self.chartView.addSubview(self.intensityChart!)
    }
    
    override func layoutSubviews() {
        self.chartView.frame = CGRectMake(15, 40, self.frame.width - 40, 45)
        self.intensityChart!.frame = self.chartView.bounds
        self.intensityChart!.reloadData()
    }
    
    func setSessionSummary(sessionSummary: Exercise.SessionSummary) {
        self.sessionSummary = sessionSummary
        let props = sessionSummary.sessionProps
        
        self.titleLabel.textColor = props.intendedIntensity.textColor()
        let mgs = Exercise.MuscleGroup.titlesFromMuscleGroupKeys(props.muscleGroupKeys, groups: LiftServerCache.sharedInstance.exerciseGetMuscleGroups())
        self.titleLabel.text = ", ".join(mgs)
        let dateText = NSDateFormatter.localizedStringFromDate(props.startDate, dateStyle: NSDateFormatterStyle.LongStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
        self.detailLabel.text = "On \(dateText)"
        self.intensityChart!.reloadData()
    }
    
    func numberOfBarsInBarChartView(barChartView: JBBarChartView!) -> UInt {
        if self.sessionSummary != nil {
            return UInt(self.sessionSummary!.setIntensities.count)
        }
        return 0
    }
    
    func barChartView(barChartView: JBBarChartView!, colorForBarViewAtIndex index: UInt) -> UIColor! {
        return self.sessionSummary!.setIntensities[Int(index)].textColor()
    }

    func barChartView(barChartView: JBBarChartView!, heightForBarViewAtIndex index: UInt) -> CGFloat {
        return CGFloat(self.sessionSummary!.setIntensities[Int(index)])
    }
    
    func barChartView(barChartView: JBBarChartView!, didSelectBarAtIndex index: UInt) {
        self.selected = true
    }
}

class OfflineSessionTableViewCell : UITableViewCell {
    
    override func awakeFromNib() {
    }
    
    func setOfflineExerciseSession(session: OfflineExerciseSession, isReplaying: Bool) -> Void {
        let mgs = Exercise.MuscleGroup.muscleGroupsFromMuscleGroupKeys(session.props.muscleGroupKeys, groups: LiftServerCache.sharedInstance.exerciseGetMuscleGroups())
        textLabel!.text = ", ".join(mgs.map { $0.title })
        let dateText = NSDateFormatter.localizedStringFromDate(session.props.startDate, dateStyle: NSDateFormatterStyle.LongStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
        detailTextLabel!.text = "On \(dateText)" // ", ".join(mgs.map { ", ".join($0.exercises) })
        setIsReplaying(isReplaying)
    }
    
    func setIsReplaying(isReplaying: Bool) {
        if isReplaying {
            if accessoryView == nil {
                accessoryView = UIActivityIndicatorView(activityIndicatorStyle: .Gray)
            }
            (accessoryView! as UIActivityIndicatorView).startAnimating()
        } else {
            accessoryView = nil
        }
    }
}

class HomeController : UIViewController, UITableViewDataSource,
    UITableViewDelegate, UIActionSheetDelegate, JTCalendarDataSource, RemoteNotificationDelegate, LiftServerDelegate {
    
    @IBOutlet var tableView: UITableView!
    @IBOutlet var calendarContentView: JTCalendarContentView!

    private var sessionSummaries: [Exercise.SessionSummary] = []
    private var sessionDates: [Exercise.SessionDate] = []
    private var sessionSuggestions: [Exercise.SessionSuggestion] = [
        Exercise.SessionSuggestion(muscleGroupKeys: ["arms"], intendedIntensity: 0.6),
        Exercise.SessionSuggestion(muscleGroupKeys: ["chest"], intendedIntensity: 0.8)
    ]
    private var offlineSessions: [OfflineExerciseSession] = []
    private let calendar = JTCalendar()

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.allowsMultipleSelectionDuringEditing = false
        calendar.calendarAppearance.isWeekMode = true
        calendar.menuMonthsView = JTCalendarMenuView()
        calendar.contentView = calendarContentView
        
        calendar.dataSource = self
    }
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.userGetPublicProfile(CurrentLiftUser.userId!) { $0.getOrUnit { publicProfile in
            if let x = publicProfile {
                self.navigationItem.title = x.firstName + " " + x.lastName
            } else {
                self.navigationItem.title = "Home".localized()
            }
            }
        }
        LiftServer.sharedInstance.exerciseGetExerciseSessionsDates(CurrentLiftUser.userId!) { $0.getOrUnit { x in
            self.sessionDates = x
            self.calendar.reloadData()
            self.calendar.currentDate = NSDate()
            self.calendar.currentDateSelected = NSDate()
            self.calendarDidDateSelected(self.calendar, date: NSDate())
            }
        }
        offlineSessions = ExerciseSessionManager.sharedInstance.listOfflineSessions()
        tableView.reloadData()
        replayOfflineSessions()
        AppDelegate.becomeCurrentRemoteNotificationDelegate(self)
        AppDelegate.becomeCurrentLiftServerDelegate(self)
    }
    
    override func viewDidDisappear(animated: Bool) {
        super.viewDidDisappear(animated)
        AppDelegate.unbecomeCurrentRemoteNotificationDelegate()
        AppDelegate.unbecomeCurrentLiftServerDelegate()
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        switch segue.identifier {
        case .Some("sessionDetail"):
            let summary = sessionSummaries[tableView.indexPathForSelectedRow()!.row]
            LiftServer.sharedInstance.exerciseGetExerciseSession(CurrentLiftUser.userId!, sessionId: summary.id) { $0.getOrUnit { x in
                if let ctrl = segue.destinationViewController as? SessionDetailController {
                    ctrl.setExerciseSession(x)
                }
                }
            }
        case .Some("startSession"):
            if let ctrl = segue.destinationViewController as? ExerciseSessionSettable {
                if let session = sender as? ExerciseSession {
                    ctrl.setExerciseSession(session)
                }
            }
        default: return
        }
    }
    
    // MARK: UITableViewDelegate
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        switch indexPath.section {
        case 0:
            let sessionSuggestion = sessionSuggestions[tableView.indexPathForSelectedRow()!.row]
            let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: sessionSuggestion.muscleGroupKeys, intendedIntensity: sessionSuggestion.intendedIntensity)
            
            LiftServer.sharedInstance.exerciseSessionStart(CurrentLiftUser.userId!, props: props) { x in
                let (isOfflineFromStart, session) = x.fold(
                    (true, ExerciseSession(id: NSUUID(), props: props)),
                    f: { x in (false, ExerciseSession(id: x, props: props)) }
                )
                let managedSession = ExerciseSessionManager.sharedInstance.managedSession(session, isOfflineFromStart: isOfflineFromStart)
                self.performSegueWithIdentifier("startSession", sender: managedSession)
            }
        case 1: performSegueWithIdentifier("sessionDetail", sender: self)
        case 2: return
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, canEditRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return indexPath.section == 1 || indexPath.section == 2
    }
    
    func tableView(tableView: UITableView, commitEditingStyle editingStyle: UITableViewCellEditingStyle, forRowAtIndexPath indexPath: NSIndexPath) {
        if editingStyle == UITableViewCellEditingStyle.Delete {
            switch indexPath.section {
            case 1:
                let sessionId = sessionSummaries[indexPath.row].id
                LiftServer.sharedInstance.exerciseDeleteExerciseSession(CurrentLiftUser.userId!, sessionId: sessionId) { _ in
                    self.sessionSummaries = self.sessionSummaries.filter { $0.id != sessionId }
                    self.tableView.reloadData()
                }
            case 2:
                let offlineSessionId = offlineSessions[indexPath.row].id
                ExerciseSessionManager.sharedInstance.removeOfflineSession(offlineSessionId)
                offlineSessions = ExerciseSessionManager.sharedInstance.listOfflineSessions()
                tableView.reloadData()
            default: fatalError("Match error")
            }
        }
    }
        
    // MARK: UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 3
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return sessionSuggestions.count
        case 1: return sessionSummaries.count
        case 2: return offlineSessions.count
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "HomeController.SessionSuggestions".localized()
        case 1: return "HomeController.SessionSummaries".localized()
        case 2: return "HomeController.OfflineSessions".localized()
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 40
        case 1: return 90
        case 2: return 40
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        switch (indexPath.section, indexPath.row) {
        case (0, let x):
            let cell = tableView.dequeueReusableCellWithIdentifier("suggestion") as UITableViewCell
            let suggestion = sessionSuggestions[x]
            let mgs = Exercise.MuscleGroup.muscleGroupsFromMuscleGroupKeys(suggestion.muscleGroupKeys, groups: LiftServerCache.sharedInstance.exerciseGetMuscleGroups())
            cell.textLabel!.text = ", ".join(mgs.map { $0.title })
            cell.detailTextLabel!.text = ", ".join(mgs.map { ", ".join($0.exercises) })
            return cell
        case (1, let x):
            let sessionSummary = sessionSummaries[x]
            let cell = tableView.dequeueReusableCellWithIdentifier("session") as SessionTableViewCell
            cell.setSessionSummary(sessionSummary)
            return cell
        case (2, let x):
            let cell = tableView.dequeueReusableCellWithIdentifier("offlineSession") as OfflineSessionTableViewCell
            let offlineSession = offlineSessions[x]
            cell.setOfflineExerciseSession(offlineSession, isReplaying: ExerciseSessionManager.sharedInstance.isReplaying(offlineSession.id))
            return cell
        default: fatalError("Match error")
        }
    }
    
    private func replayOfflineSessions() {
        for (index, offlineSession) in enumerate(offlineSessions) {
            if !ExerciseSessionManager.sharedInstance.isReplaying(offlineSession.id) {
                if let cell = tableView.cellForRowAtIndexPath(NSIndexPath(forRow: index, inSection: 2)) as? OfflineSessionTableViewCell {
                    cell.setIsReplaying(true)
                    NSLog("Replaying \(index)")
                    
                    ExerciseSessionManager.sharedInstance.replayOfflineSession(offlineSession.id, removeAfterSuccess: true) {
                        $0.cata(
                            { _ in cell.setIsReplaying(false) },
                            { _ in
                                self.offlineSessions = ExerciseSessionManager.sharedInstance.listOfflineSessions()
                                self.tableView.reloadSections(NSIndexSet(index: 2), withRowAnimation: UITableViewRowAnimation.Automatic)
                                self.replayOfflineSessions()
                            }
                        )
                    }
                    
                    return   // we replay only one at a time
                }
            }
        }
    }
    
    // MARK: JTCalendarDataSource
    
    func calendarHaveEvent(calendar: JTCalendar!, date: NSDate!) -> Bool {
        return !sessionDates.filter { elem in return elem.date == date }.isEmpty
    }
    
    func calendarDidDateSelected(calendar: JTCalendar!, date: NSDate!) {
        LiftServer.sharedInstance.exerciseGetExerciseSessionsSummary(CurrentLiftUser.userId!, date: date) { $0.getOrUnit { x in
            self.sessionSummaries = x
            self.tableView.reloadData()
            }
        }
    }

    
    // MARK: UIActionSheetDelegate
    func actionSheet(actionSheet: UIActionSheet, clickedButtonAtIndex buttonIndex: Int) {
        if buttonIndex == 0 {
            performSegueWithIdentifier("logout", sender: self)
        }
    }
    
    // MARK: RemoteNotificationDelegate
    func remoteNotificationReceived(#alert: String) {
        calendarDidDateSelected(self.calendar, date: NSDate())
    }
    
    func remoteNotificatioReceived(#data: NSData) {
        let json = JSON(data: data)
        if json["type"].string == "offline-management" {
            if json["replay"].bool == true { replayOfflineSessions() }
        }
    }
    
    // MARK: LiftServerDelegate
    func liftServer(liftServer: LiftServer, availabilityStateChanged newState: AvailabilityState) {
        if !newState.isOnline() { return }
        
        replayOfflineSessions()
    }
    
    // MARK: Actions
    @IBAction
    func editProfile() {
        performSegueWithIdentifier("profile", sender: self)
    }
    
    @IBAction
    func settings() {
        let menu = UIActionSheet(title: nil, delegate: self, cancelButtonTitle: "Cancel".localized(), destructiveButtonTitle: "Logout".localized())
        menu.showFromTabBar(tabBarController?.tabBar)
    }
    
}