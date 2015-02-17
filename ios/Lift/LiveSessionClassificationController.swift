import Foundation

class LiveSessionClassificationController : UITableViewController, ExerciseSessionSettable, UIActionSheetDelegate {
    private var classificationExamples: [Exercise.Exercise] = []
    private var session: ExerciseSession!
    private var popoverContentController: LiveSessionClassificationPopoverController!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        popoverContentController = LiveSessionClassificationPopoverController(nibName: "LiveSessionClassificationPopoverController", bundle: nil)
        popoverContentController.view.frame = CGRectMake(0, 0, 320, 60)
    }
    
    // MARK: UIActionSheetDelegate implementation
    func willPresentActionSheet(actionSheet: UIActionSheet) {
        actionSheet.addSubview(popoverContentController.view)
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
            let sheet = UIActionSheet(title: nil, delegate: self, cancelButtonTitle: nil, destructiveButtonTitle: nil)
            let height: CGFloat = 100.0
            sheet.showInView(view)
            sheet.bounds = CGRectMake(0, 0, 320, 600)
            
            tableView.deselectRowAtIndexPath(indexPath, animated: false)
        default: return
        }
    }
}
