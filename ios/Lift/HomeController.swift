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

class HomeController : UIParallaxViewController, UITableViewDataSource,
    UITableViewDelegate, HomeControllerHeaderViewDelegate, UIActionSheetDelegate {
    
    @IBOutlet var tableView: UITableView!
    private var sessionSummaries: [Exercise.SessionSummary] = []
    private var sessionDates: [Exercise.SessionDate] = []
    private var sessionSuggestions: [Exercise.SessionSuggestion] = [
        Exercise.SessionSuggestion(muscleGroupKeys: ["arms"], intendedIntensity: 0.6),
        Exercise.SessionSuggestion(muscleGroupKeys: ["chest"], intendedIntensity: 0.8)
    ]
    private var headerView: HomeControllerHeaderView!
    
    override func contentView() -> UIScrollView {
        return tableView
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.scrollEnabled = false
        headerView = NSBundle.mainBundle().loadNibNamed("HomeControllerHeader", owner: self, options: nil).first as HomeControllerHeaderView
        headerView.setDelegate(self)
        addHeaderOverlayView(headerView)
    }
    
    override func viewDidAppear(animated: Bool) {
        ResultContext.run { ctx in
            LiftServer.sharedInstance.userGetPublicProfile(CurrentLiftUser.userId!, ctx.apply(self.headerView.setPublicProfile))
            LiftServer.sharedInstance.userGetProfileImage(CurrentLiftUser.userId!, ctx.apply { data in
                if let image = UIImage(data: data) {
                    self.setHeaderImage(image)
                    self.headerView.setProfileImage(image)
                }
            })
            LiftServer.sharedInstance.exerciseGetExerciseSessionsDates(CurrentLiftUser.userId!, ctx.apply { x in
                self.sessionDates = x
                self.headerView.reloadData()
            })
        }
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        switch segue.identifier {
        case .Some("sessionDetail"):
            let summary = sessionSummaries[tableView.indexPathForSelectedRow()!.row]
            ResultContext.run { ctx in
                LiftServer.sharedInstance.exerciseGetExerciseSession(CurrentLiftUser.userId!, sessionId: summary.id, ctx.apply { x in
                    if let ctrl = segue.destinationViewController as? SessionDetailController {
                        ctrl.setExerciseSession(x)
                    }
                    })
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
            ResultContext.run { ctx in
                LiftServer.sharedInstance.exerciseSessionStart(CurrentLiftUser.userId!, props: props, ctx.apply { sessionId in
                    let session = ExerciseSession(id: sessionId, props: props)
                    self.performSegueWithIdentifier("startSession", sender: session)
                    })
            }
        case 1: performSegueWithIdentifier("sessionDetail", sender: self)
        default: fatalError("Match error")
        }
    }
        
    // MARK: UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return sessionSuggestions.count
        case 1: return sessionSummaries.count
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "HomeController.SessionSuggestions".localized()
        case 1: return "HomeController.SessionSummaries".localized()
        default: fatalError("Match error")
        }
    }
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 40
        case 1: return 90
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
        default: fatalError("Match error")
        }
    }
    
    // MARK: UIActionSheetDelegate
    func actionSheet(actionSheet: UIActionSheet, clickedButtonAtIndex buttonIndex: Int) {
        if buttonIndex == 0 {
            performSegueWithIdentifier("logout", sender: self)
        }
    }
    
    // MARK: HomeControllerHeaderViewDelegate
    
    func headerDateSelected(date: NSDate) {
        ResultContext.run { ctx in
            LiftServer.sharedInstance.exerciseGetExerciseSessionsSummary(CurrentLiftUser.userId!, date: date, ctx.apply { x in
                self.sessionSummaries = x
                self.tableView.reloadData()
                self.scrollToTop()
            })
        }
    }
    
    func headerSessionsOnDate(date: NSDate) -> Exercise.SessionDate? {
        return sessionDates.filter { elem in return elem.date == date }.first
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