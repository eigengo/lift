import Foundation

class LiveSessionClassificationController : UITableViewController, ExerciseSessionSettable, UIAlertViewDelegate {
    private var classificationExamples: [Exercise.Exercise] = []
    private var session: ExerciseSession!
    private var popoverContentController: LiveSessionClassificationPopoverController!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        popoverContentController = LiveSessionClassificationPopoverController(nibName: "LiveSessionClassificationPopoverController", bundle: nil)
        popoverContentController.view.frame = CGRectMake(0, 0, 320, 60)
    }
    
    // MARK: ExerciseSessionSettable implementation
    func setExerciseSession(session: ExerciseSession) {
        self.session = session
        self.session.getClassificationExamples { $0.getOrUnit { x in
                self.classificationExamples = x
                self.tableView.reloadData()
            }
        }
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return classificationExamples.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        switch (indexPath.section, indexPath.row) {
        case (0, let x):
            let cell = tableView.dequeueReusableCellWithIdentifier("manual") as UITableViewCell
            cell.textLabel!.text = classificationExamples[x].name
            return cell
        default: fatalError("Match error")
        }
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        switch (indexPath.section, indexPath.row) {
        case (0, let x):
            let c = UIAlertController(title: nil, message: nil, preferredStyle: UIAlertControllerStyle.ActionSheet)
            c.view = popoverContentController.view
            presentViewController(c, animated: true, completion: nil)
            
            tableView.deselectRowAtIndexPath(indexPath, animated: false)
        default: return
        }
    }
}
