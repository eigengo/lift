import Foundation

///
/// Table data source
///
class MuscleGroupsTableModel : NSObject, UITableViewDataSource {
    private var muscleGroups: [Exercise.MuscleGroup] = []

    init(f: Void -> Void) {
        super.init()
        LiftServer.sharedInstance.exerciseGetMuscleGroups {
            self.muscleGroups = $0.fold([], identity)
            f()
        }
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

}

class NewSessionController : UIViewController, UITableViewDelegate {
    @IBOutlet
    var demoMode: UISwitch!
    @IBOutlet
    var tableView: UITableView!
    var tableModel: MuscleGroupsTableModel?

    override func viewDidLoad() {
        self.tableModel = MuscleGroupsTableModel(self.tableView.reloadData)
        tableView.dataSource = self.tableModel!
        tableView.delegate = self
    }
    
    func tableView(tableView: UITableView, accessoryButtonTappedForRowWithIndexPath indexPath: NSIndexPath) {
        NSLog("Show info for cell")
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let cell = tableModel!.muscleGroupAt(indexPath)
        if cell != nil {
            startSession([cell!.key])
        }
    }
    
    func startSession(muscleGroupKeys: [String]) -> Void {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: muscleGroupKeys, intendedIntensity: 0.7)
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