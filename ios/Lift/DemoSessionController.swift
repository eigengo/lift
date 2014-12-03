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

class DemoSessionController : UIViewController, UITabBarDelegate, UITableViewDelegate, ExerciseSessionSettable {
    @IBOutlet
    var tableView: UITableView!
    var tableModel: DemoSessionTableModel?
    var sessionId: NSUUID?
    
    override func viewDidLoad() {
        tableView.delegate = self
        tableView.dataSource = tableModel!
    }
    
    func setExerciseSession(session: ExerciseSession) {
        self.sessionId = session.id
        tableModel = DemoSessionTableModel(muscleGroupKeys: session.props.muscleGroupKeys)
    }
    
    func tabBar(tabBar: UITabBar, didSelectItem item: UITabBarItem!) {
        // we only have one item, so back we go
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: sessionId!) {
            $0.cata(LiftAlertController.showError("sessionend_fail", parent: self), const(self.performSegueWithIdentifier("end", sender: nil)))
        }
        
    }
 
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let path = tableModel!.filePathAtIndexPath(indexPath)
        let data = NSFileManager.defaultManager().contentsAtPath(path!)
        LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: self.sessionId!, data: data!) { x in
            NSLog("Sent...")
        }
        
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
    }
}