import Foundation

class NewSessionController : UIViewController, UITableViewDelegate, UITableViewDataSource {
    @IBOutlet
    var demoMode: UISwitch!
    @IBOutlet
    var tableView: UITableView!
    private var muscleGroups: [Exercise.MuscleGroup] = []

    override func viewDidLoad() {
        tableView.dataSource = self
        tableView.delegate = self
    }
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.exerciseGetMuscleGroups {
            self.muscleGroups = $0.cata(const([]), identity)
            self.tableView.reloadData()
        }
    }
    
    func tableView(tableView: UITableView, accessoryButtonTappedForRowWithIndexPath indexPath: NSIndexPath) {
        NSLog("Show info for cell")
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let cell = muscleGroups[indexPath.row]
        startSession([cell.key])
    }
    
    func muscleGroupAt(indexPath: NSIndexPath) -> Exercise.MuscleGroup? {
        return muscleGroups[indexPath.row]
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
    
    func startSession(muscleGroupKeys: [String]) -> Void {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: muscleGroupKeys, intendedIntensity: 0.75)
        LiftServer.sharedInstance.exerciseSessionStart(CurrentLiftUser.userId!, props: props) {
            $0.cata(LiftAlertController.showError("startsession_failed", parent: self), self.segueToStartedSession(props))
        }
    }
    
    func segueToStartedSession(props: Exercise.SessionProps) -> NSUUID -> Void {
        return { sessionId -> Void in
            let segueName = self.demoMode.on ? "newsession_demo" : "newsession_live"
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