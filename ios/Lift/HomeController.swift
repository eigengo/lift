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

class HomeController : UIParallaxViewController, UITableViewDataSource, UITableViewDelegate {
    @IBOutlet var tableView: UITableView!
    private var sessionSummaries: [Exercise.SessionSummary] = []
    
    override func contentView() -> UIScrollView {
        return tableView
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        //navigationController!.navigationBarHidden = true
        tableView.scrollEnabled = false
        let view = NSBundle.mainBundle().loadNibNamed("HomeControllerHeader", owner: self, options: nil).first as UIView
        view.backgroundColor = UIColor.clearColor()
        view.alpha = 1
        setHeaderImage(UIImage(named: "user1")!)
        addHeaderOverlayView(view)
    }
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.exerciseGetExerciseSessionsSummary(CurrentLiftUser.userId!) {
            self.sessionSummaries = $0.fold([], identity)
            self.tableView.reloadData()
        }
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        performSegueWithIdentifier("detail", sender: self)
    }
    
    func showSessionDetail(segue: UIStoryboardSegue) -> Exercise.ExerciseSession -> Void {
        return { exerciseSession in
            if let ctrl = segue.destinationViewController as? SessionDetailController {
                ctrl.setExerciseSession(exerciseSession)
            }
        }
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        let summary = sessionSummaries[tableView.indexPathForSelectedRow()!.row]
        LiftServer.sharedInstance.exerciseGetExerciseSession(CurrentLiftUser.userId!, sessionId: summary.id) {
            $0.cata(LiftAlertController.showError("session_detail_failed", parent: self), self.showSessionDetail(segue))
        }
    }

    // MARK: UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sessionSummaries.count
    }
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        return 90
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let sessionSummary = sessionSummaries[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as SessionTableViewCell
        cell.setSessionSummary(sessionSummary)
        return cell
    }

}