import Foundation

class DemoSessionTableModel : NSObject, UITableViewDataSource {
    let dataFiles = ["foo", "bar", "baz"]
    
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
        
        cell.textLabel!.text = data
        cell.detailTextLabel!.text = "xyz B"
        
        return cell
    }

}

class DemoSessionController : UIViewController, UITabBarDelegate, UITableViewDelegate, MuscleGroupsSettable {
    @IBOutlet
    var tableView: UITableView!
    let tableModel = DemoSessionTableModel()
    
    override func viewDidLoad() {
        tableView.delegate = self
        tableView.dataSource = tableModel
    }
    
    func setMuscleGroupKeys(muscleGroupKeys: [String]) {
        NSLog("Starting with %@", muscleGroupKeys)
    }
    
    func tabBar(tabBar: UITabBar, didSelectItem item: UITabBarItem!) {
        // we only have one item, so back we go
        performSegueWithIdentifier("end", sender: nil)
    }
 
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        NSLog("Send ADs")
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
    }
}