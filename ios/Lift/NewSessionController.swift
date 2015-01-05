import Foundation

class NewSessionMuscleGroupsController : UIViewController, UITableViewDelegate, UITableViewDataSource {
    @IBOutlet
    var demoMode: UISwitch!
    @IBOutlet
    var tableView: UITableView!
    private var muscleGroups: [Exercise.MuscleGroup] = []

    override func viewDidAppear(animated: Bool) {
        ResultContext.run { ctx in
            LiftServer.sharedInstance.exerciseGetMuscleGroups(ctx.apply { x in
                self.muscleGroups = x
                self.tableView.reloadData()
            })
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
            Exercise.ExerciseIntensity(intensity: 0.45, title: "Light",      userNotes: "Basic endurance."),
            Exercise.ExerciseIntensity(intensity: 0.65, title: "Moderate",   userNotes: "Aerobic fitness improvement."),
            Exercise.ExerciseIntensity(intensity: 0.75, title: "Hard",       userNotes: "FTP improvement."),
            Exercise.ExerciseIntensity(intensity: 0.87, title: "Very hard",  userNotes: "Maximum performance development."),
            Exercise.ExerciseIntensity(intensity: 0.95, title: "Brutal",     userNotes: "Bleeding eyes!")
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
        ResultContext.run { ctx in
            LiftServer.sharedInstance.exerciseSessionStart(CurrentLiftUser.userId!, props: props, ctx.apply { x in self.segueToStartedSession(props, sessionId: x) })
        }
    }
    
    func segueToStartedSession(props: Exercise.SessionProps, sessionId: NSUUID) -> Void {
        let segueName = self.demoMode ? "demo" : "live"
        let session = ExerciseSession(id: sessionId, props: props)
        self.performSegueWithIdentifier(segueName, sender: session)
    }

    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if let ctrl = segue.destinationViewController as? ExerciseSessionSettable {
            if let session = sender as? ExerciseSession {
                ctrl.setExerciseSession(session)
            }
        }
    }

}