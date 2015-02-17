import Foundation

class LiveSessionClassificationCell : UITableViewCell {
    struct Height {
        static let expanded: CGFloat = 185
        static let collapsed: CGFloat = 40
    }
    
    @IBOutlet
    var titleLabel: UILabel!

    func setExercise(exercise: Exercise.Exercise) {
        titleLabel.text = exercise.name
    }
    
}

class LiveSessionClassificationController : UITableViewController, ExerciseSessionSettable, UIAlertViewDelegate {
    private var classificationExamples: [Exercise.Exercise] = []
    private var session: ExerciseSession!
    private var selectedIndexPath: NSIndexPath?
    
    override func viewDidLoad() {
        super.viewDidLoad()
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
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        if let sip = selectedIndexPath {
            if sip == indexPath { return LiveSessionClassificationCell.Height.expanded }
            return LiveSessionClassificationCell.Height.collapsed
        }
        
        return 40
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return classificationExamples.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        switch (indexPath.section, indexPath.row) {
        case (0, let x):
            let cell = tableView.dequeueReusableCellWithIdentifier("manual") as LiveSessionClassificationCell
            cell.setExercise(classificationExamples[x])
            return cell
        default: fatalError("Match error")
        }
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if selectedIndexPath == .Some(indexPath) {
            selectedIndexPath = nil
        } else {
            selectedIndexPath = indexPath
        }
        tableView.reloadRowsAtIndexPaths([indexPath], withRowAnimation: UITableViewRowAnimation.Fade)
    }
    
}
