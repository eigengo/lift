import Foundation

protocol LiveSessionClassificationCellDelegate {
    
    func repetitions(count: Int, of exercise: Exercise.Exercise)
    
}

class LiveSessionClassificationCell : UITableViewCell {
    struct Height {
        static let expanded: CGFloat = 190
        static let collapsed: CGFloat = 40
    }
    private var delegate: LiveSessionClassificationCellDelegate?
    
    @IBOutlet
    var titleLabel: UILabel!
    private var exercise: Exercise.Exercise!

    func setExercise(exercise: Exercise.Exercise) {
        titleLabel.text = exercise.name
        self.exercise = exercise
    }
    
    @IBAction
    func repetition(sender: UIButton) {
        delegate?.repetitions(sender.tag, of: exercise)
    }
    
}

class LiveSessionClassificationController : UITableViewController, ExerciseSessionSettable, LiveSessionClassificationCellDelegate {
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
            cell.delegate = self
            return cell
        default: fatalError("Match error")
        }
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if selectedIndexPath == .Some(indexPath) {
            session.endExplicitClassification()
            selectedIndexPath = nil
        } else {
            if selectedIndexPath != nil { session.endExplicitClassification() }
            let exercise = classificationExamples[indexPath.row]
            session.startExplicitClassification(exercise)
            selectedIndexPath = indexPath
        }
        tableView.reloadRowsAtIndexPaths([indexPath], withRowAnimation: UITableViewRowAnimation.Fade)
    }
    
    // MARK: LiveSessionClassificationCellDelegate code
    func repetitions(count: Int, of exercise: Exercise.Exercise) {
        // note that we're sending one fewer request
        // the act of selecting the cell has already sent one
        for _ in 1..<count {
            session.startExplicitClassification(exercise)
        }
    }
    
}
