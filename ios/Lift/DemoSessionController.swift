import Foundation

struct DataFile {
    var path: String
    var size: NSNumber
    var name: String
}

class DemoSessionTableModel : NSObject, UITableViewDataSource {
    private var dataFiles: [DataFile]
    
    init(muscleGroupKeys: [String]) {
        dataFiles = NSBundle.mainBundle().pathsForResourcesOfType(".adv1", inDirectory: nil)
            .map { p -> String in return p as String }
            .filter { p in return !muscleGroupKeys.filter { k in return p.lastPathComponent.hasPrefix(k) }.isEmpty }
            .map { path in
                let attrs = NSFileManager.defaultManager().attributesOfItemAtPath(path, error: nil)!
                let size: NSNumber = attrs[NSFileSize] as NSNumber
                return DataFile(path: path, size: size, name: path.lastPathComponent)
            }
        super.init()
    }
    
    // #pragma mark - UITableViewDataSource
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
    private let reallyStopTag = 1
    private var timer: NSTimer?
    private var startTime: NSDate?
    
    override func viewDidLoad() {
        self.tableView.delegate = self
        self.tableView.dataSource = tableModel!
        self.stopSessionButton.title = "Stop"
        self.stopSessionButton.tag = 0
        self.startTime = NSDate()
        self.timer = NSTimer.scheduledTimerWithTimeInterval(1, target: self, selector: "tick", userInfo: nil, repeats: true)
    }
    
    func tick() {
        let elapsed = Int(NSDate().timeIntervalSinceDate(self.startTime!))
        let minutes: Int = elapsed / 60
        let seconds: Int = elapsed - minutes * 60
        self.navigationItem.prompt = NSString(format: "Elapsed %d:%02d", minutes, seconds)
    }
    
    func setExerciseSession(session: ExerciseSession) {
        self.sessionId = session.id
        self.tableModel = DemoSessionTableModel(muscleGroupKeys: session.props.muscleGroupKeys)
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let path = self.tableModel!.filePathAtIndexPath(indexPath)
        let data = NSFileManager.defaultManager().contentsAtPath(path!)
        LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: self.sessionId!, data: data!) { x in
            NSLog("Sent...")
        }
        
        self.tableView.deselectRowAtIndexPath(indexPath, animated: true)
    }
    
    override func viewWillDisappear(animated: Bool) {
        self.timer!.invalidate()
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: self.sessionId!) { _ in }
    }
    
    @IBAction
    func stopSession() {
        if self.stopSessionButton.tag != reallyStopTag {
            self.stopSessionButton.title = "Really?"
            self.stopSessionButton.tag = reallyStopTag
        } else {
            self.navigationItem.prompt = nil
            self.navigationController!.popToRootViewControllerAnimated(true)
        }
    }
}