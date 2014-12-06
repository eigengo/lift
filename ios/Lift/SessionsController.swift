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
        self.intensityChart!.frame = self.chartView.frame
        addSubview(self.intensityChart!)
    }
    
    func setSessionSummary(sessionSummary: Exercise.SessionSummary) {
        self.sessionSummary = sessionSummary
        let props = sessionSummary.sessionProps
        
        self.titleLabel.textColor = props.intendedIntensity.textColor()
        let dateText = NSDateFormatter.localizedStringFromDate(props.startDate, dateStyle: NSDateFormatterStyle.MediumStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
        // let ints = " ".join(sessionSummary.setIntensities.map { x in return String(format:"%f", x) })
        self.detailLabel.text = "On \(dateText)"
        self.intensityChart!.reloadData()
    }
    
    func numberOfBarsInBarChartView(barChartView: JBBarChartView!) -> UInt {
        if self.sessionSummary != nil {
            return 5 //UInt(self.sessionSummary!.setIntensities.count)
        }
        return 0
    }

    func barChartView(barChartView: JBBarChartView, colorForBarViewAtIndex index: UInt) -> UIColor {
        return self.sessionSummary!.setIntensities[0].textColor()
    }

    func barChartView(barChartView: JBBarChartView!, heightForBarViewAtIndex index: UInt) -> CGFloat {
        return 100 //CGFloat(self.sessionSummary!.setIntensities[Int(index)]) * 20
    }
}

class SessionsController : UITableViewController, UITableViewDataSource {
    private var sessionSummaries: [Exercise.SessionSummary] = []
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.exerciseGetExerciseSessionsSummary(CurrentLiftUser.userId!) {
            self.sessionSummaries = $0.fold([], identity)
            self.tableView.reloadData()
        }
    }
    
    override func viewDidLoad() {
        self.tableView.registerNib(UINib(nibName: "SessionTableCell", bundle: nil), forCellReuseIdentifier: "default")
    }

    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
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

    // #pragma mark - UITableViewDataSource
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sessionSummaries.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let sessionSummary = sessionSummaries[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as SessionTableViewCell
        cell.setSessionSummary(sessionSummary)
        return cell
    }
    

}