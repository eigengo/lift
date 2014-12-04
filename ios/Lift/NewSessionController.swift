import Foundation

class NewSessionMuscleGroupsController : UIViewController, UITableViewDelegate, UITableViewDataSource {
    @IBOutlet
    var demoMode: UISwitch!
    @IBOutlet
    var tableView: UITableView!
    private var muscleGroups: [Exercise.MuscleGroup] = []

    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.exerciseGetMuscleGroups {
            self.muscleGroups = $0.cata(const([]), identity)
            self.tableView.reloadData()
        }
    }
    
    // #pragma mark - UITableViewDelegate
    func tableView(tableView: UITableView, accessoryButtonTappedForRowWithIndexPath indexPath: NSIndexPath) {
        NSLog("Show info for cell")
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let cell = muscleGroups[indexPath.row]
        performSegueWithIdentifier("sessionProps", sender: [cell.key])
    }
    
    // #pragma mark - UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return muscleGroups.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let data = muscleGroups[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as UITableViewCell
        
        cell.textLabel!.text = data.title
        cell.detailTextLabel!.text = ", ".join(data.exercises)
        
        return cell
    }
    
    // #pragma MARK - the rest
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if let ctrl = segue.destinationViewController as? NewSessionPropsController {
            if let muscleGroupsKeys = sender as? [Exercise.MuscleGroupKey] {
                ctrl.muscleGroupKeys = muscleGroupsKeys
                ctrl.demoMode = self.demoMode.on
            }
        }
    }
}

class NewSessionPropsController : UIViewController, UITableViewDelegate, UITableViewDataSource {
    var intensities: [Exercise.ExerciseIntensity] = []
    var muscleGroupKeys: [Exercise.MuscleGroupKey] = []
    var demoMode: Bool = true
    @IBOutlet
    var tableView: UITableView!

    override func viewDidAppear(animated: Bool) {
        // TODO: Lift server: get intensities
        intensities = [
            Exercise.ExerciseIntensity(intensity: 0.30, title: "Very light", userNotes: "Just stretching bones on a rest day."),
            Exercise.ExerciseIntensity(intensity: 0.45, title: "Light",      userNotes: "Basic endurance. Energy from fat."),
            Exercise.ExerciseIntensity(intensity: 0.65, title: "Moderate",   userNotes: "Aerobic fitness improvement."),
            Exercise.ExerciseIntensity(intensity: 0.75, title: "Hard",       userNotes: "Improves functional power."),
            Exercise.ExerciseIntensity(intensity: 0.87, title: "Very hard",  userNotes: "Develop maximum performance and speed."),
            Exercise.ExerciseIntensity(intensity: 0.95, title: "Brutal",     userNotes: "Do you feel lucky, punk?")
        ]
        self.tableView.reloadData()
    }

    // #pragma mark - UITableViewDelegate
    func tableView(tableView: UITableView, accessoryButtonTappedForRowWithIndexPath indexPath: NSIndexPath) {
        NSLog("Show info for cell")
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let cell = intensities[indexPath.row]
        startSession(self.muscleGroupKeys, intensity: cell)
    }
    
    // #pragma mark - UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return intensities.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let data = intensities[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as UITableViewCell
        
        cell.textLabel!.textColor = data.intensity.textColor()
        cell.textLabel!.text = data.title
        cell.detailTextLabel!.text = data.userNotes
        
        return cell
    }
    
    func startSession(muscleGroupKeys: [Exercise.MuscleGroupKey], intensity: Exercise.ExerciseIntensity) -> Void {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: muscleGroupKeys, intendedIntensity: intensity.intensity)
        LiftServer.sharedInstance.exerciseSessionStart(CurrentLiftUser.userId!, props: props) {
            $0.cata(LiftAlertController.showError("startsession_failed", parent: self), self.segueToStartedSession(props))
        }
    }
    
    func segueToStartedSession(props: Exercise.SessionProps) -> NSUUID -> Void {
        return { sessionId -> Void in
            let segueName = self.demoMode ? "demo" : "live"
            let session = ExerciseSession(id: sessionId, props: props)
            self.performSegueWithIdentifier(segueName, sender: session)
        }
    }

    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if let ctrl = segue.destinationViewController as? ExerciseSessionSettable {
            if let session = sender as? ExerciseSession {
                ctrl.setExerciseSession(session)
            }
        }
    }

}