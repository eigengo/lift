import Foundation

struct DataFile {
    var path: String
    var size: NSNumber
    var name: String
}

class DemoSessionTableModel : NSObject, UITableViewDataSource {
    private var dataFiles: [DataFile]
    
    init(muscleGroupKeys: [String]) {
        dataFiles = NSBundle.mainBundle().pathsForResourcesOfType(".dat", inDirectory: nil)
            .map { p -> String in return p as String }
            .filter { p in return !muscleGroupKeys.filter { k in return p.lastPathComponent.hasPrefix(k) }.isEmpty }
            .map { path in
                let attrs = NSFileManager.defaultManager().attributesOfItemAtPath(path, error: nil)!
                let size: NSNumber = attrs[NSFileSize] as NSNumber
                return DataFile(path: path, size: size, name: path.lastPathComponent)
            }
        super.init()
    }
    
    // MARK: UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return dataFiles.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let data = dataFiles[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as UITableViewCell
        
        cell.textLabel!.text = data.name
        let fmt = NSNumberFormatter()
        fmt.numberStyle = NSNumberFormatterStyle.DecimalStyle
        let sizeString = fmt.stringFromNumber(data.size)!
        cell.detailTextLabel!.text = "\(sizeString) B"
        
        return cell
    }
    
    func filePathAtIndexPath(indexPath: NSIndexPath) -> String? {
        return dataFiles[indexPath.row].path
    }

}

class DemoSessionController : UIViewController, UITableViewDelegate, ExerciseSessionSettable {
    @IBOutlet var tableView: UITableView!
    @IBOutlet var stopSessionButton: UIBarButtonItem!
    
    private var tableModel: DemoSessionTableModel?
    private var sessionId: NSUUID?
    private var timer: NSTimer?
    private var startTime: NSDate?
    
    // MARK: main
    override func viewWillDisappear(animated: Bool) {
        timer!.invalidate()
        navigationItem.prompt = nil
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: sessionId!) { _ in }
    }
    
    @IBAction
    func stopSession() {
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Really?"
            stopSessionButton.tag = 3
        } else {
            navigationItem.prompt = nil
            navigationController!.popToRootViewControllerAnimated(true)
        }
    }

    override func viewDidLoad() {
        tableView.delegate = self
        tableView.dataSource = tableModel!
        stopSessionButton.title = "Stop"
        stopSessionButton.tag = 0
        startTime = NSDate()
        tabBarController?.tabBar.hidden = true
        timer = NSTimer.scheduledTimerWithTimeInterval(1, target: self, selector: "tick", userInfo: nil, repeats: true)
    }
    
    func tick() {
        let elapsed = Int(NSDate().timeIntervalSinceDate(self.startTime!))
        let minutes: Int = elapsed / 60
        let seconds: Int = elapsed - minutes * 60
        navigationItem.prompt = NSString(format: "Elapsed %d:%02d", minutes, seconds)
        stopSessionButton.tag -= 1
        if stopSessionButton.tag < 0 {
            stopSessionButton.title = "Stop"
        }
    }
    
    // MARK: ExerciseSessionSettable
    func setExerciseSession(session: ExerciseSession) {
        sessionId = session.id
        tableModel = DemoSessionTableModel(muscleGroupKeys: session.props.muscleGroupKeys)
    }
    
    // MARK: UITableViewDelegate
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let path = tableModel!.filePathAtIndexPath(indexPath)
        let data = NSFileManager.defaultManager().contentsAtPath(path!)
        LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: sessionId!, data: data!) { x in
            NSLog("Sent...")
        }
        
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
    }

}