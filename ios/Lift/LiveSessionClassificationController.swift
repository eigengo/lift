import Foundation

///
/// Implement this protocol to receive notifications of the user actions
/// on the LiveSessionClassificationCell
///
protocol LiveSessionClassificationCellDelegate {
    
    func repetitions(count: Int, of exercise: Exercise.Exercise)
    
    func intensity(key: Exercise.ExerciseIntensityKey, of exercise: Exercise.Exercise)
    
}

///
/// Displays the cell of live classification exercise, allowing the user to set
/// the repetitions, intensity, and—in the future—metric.
///
class LiveSessionClassificationCell : UITableViewCell {
    /// default cell heights
    struct Height {
        static let expanded: CGFloat = 160
        static let collapsed: CGFloat = 40
    }
    
    /// default values
    struct Defaults {
        static let repetitions: [Int] = [10, 2, 5, 8]
        static let intensities: [Exercise.ExerciseIntensityKey] = [
            Exercise.ExerciseIntensity.moderate,
            Exercise.ExerciseIntensity.light,
            Exercise.ExerciseIntensity.hard,
            Exercise.ExerciseIntensity.brutal
        ].map { $0.intensity }
    }
    
    private var delegate: LiveSessionClassificationCellDelegate?
    
    @IBOutlet var titleLabel: UILabel!
    
    @IBOutlet var defaultIntensityButton: UIButton!
    @IBOutlet var leftIntensityButton: UIButton!
    @IBOutlet var middleIntensityButton: UIButton!
    @IBOutlet var rightIntensityButton: UIButton!

    @IBOutlet var defaultRepetitionsButton: UIButton!
    @IBOutlet var leftRepetitionsButton: UIButton!
    @IBOutlet var middleRepetitionsButton: UIButton!
    @IBOutlet var rightRepetitionsButton: UIButton!

    private var repetitions: [Int]!
    private var intensities: [Exercise.ExerciseIntensityKey]!
    private var exercise: Exercise.Exercise!

    ///
    /// Update this cell with the given ``exercise``
    ///
    func setExercise(exercise: Exercise.Exercise) {
        titleLabel.text = exercise.name
        // TODO: Once statistics are wired in, show the exercise.intensity, exericse.metric and exercise.repetitions
        
        repetitions = Defaults.repetitions
        intensities = Defaults.intensities

        let allStates = UIControlState.Normal | UIControlState.Highlighted | UIControlState.Selected
        defaultIntensityButton.setTitle(intensities[0].intensity.title, forState: allStates)
        leftIntensityButton.setTitle(   intensities[1].intensity.title, forState: allStates)
        middleIntensityButton.setTitle( intensities[2].intensity.title, forState: allStates)
        rightIntensityButton.setTitle(  intensities[3].intensity.title, forState: allStates)
        
        defaultRepetitionsButton.setTitle(String(repetitions[0]), forState: allStates)
        leftRepetitionsButton.setTitle(   String(repetitions[1]), forState: allStates)
        middleRepetitionsButton.setTitle( String(repetitions[2]), forState: allStates)
        rightRepetitionsButton.setTitle(  String(repetitions[3]), forState: allStates)
        
        self.exercise = exercise
    }
    
    @IBAction
    func repetition(sender: UIButton) {
        let delta = repetitions[sender.tag]
        delegate?.repetitions(delta, of: exercise)
    }
    
    @IBAction
    func intensity(sender: UIButton) {
        let intensity = intensities[sender.tag]
        delegate?.intensity(intensity, of: exercise)
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
    
    func intensity(key: Exercise.ExerciseIntensityKey, of exercise: Exercise.Exercise) {
        // TODO
    }
    
}
