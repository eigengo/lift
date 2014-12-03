import Foundation

class SessionsController : UITableViewController, UITableViewDataSource {
    private var sessionProps: [Exercise.SessionProps] = []
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.exerciseGetAllExercises(CurrentLiftUser.userId!) {
            self.sessionProps = $0.fold([], identity)
            self.tableView.reloadData()
        }
    }

    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        performSegueWithIdentifier("detail", sender: self)
    }

    // #pragma mark - UITableViewDataSource
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sessionProps.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let data = sessionProps[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as UITableViewCell
        
        let dateText = NSDateFormatter.localizedStringFromDate(data.startDate, dateStyle: NSDateFormatterStyle.MediumStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
        cell.textLabel!.text = ", ".join(data.muscleGroupKeys)
        cell.detailTextLabel!.text = "On \(dateText)"
        
        return cell
    }
    

}