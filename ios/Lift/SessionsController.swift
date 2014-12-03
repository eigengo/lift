import Foundation

class SessionsController : UITableViewController, UITableViewDataSource {
    private var sessionSummaries: [Exercise.SessionSummary] = []
    
    override func viewDidAppear(animated: Bool) {
        LiftServer.sharedInstance.exerciseGetExerciseSessionsSummary(CurrentLiftUser.userId!) {
            self.sessionSummaries = $0.fold([], identity)
            self.tableView.reloadData()
        }
    }

    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        performSegueWithIdentifier("detail", sender: self)
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        let summary = sessionSummaries[tableView.indexPathForSelectedRow()!.row]
        if let ctrl = segue.destinationViewController as? SessionDetailController {
            ctrl.setSessionId(summary.id)
        }
    }

    // #pragma mark - UITableViewDataSource
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sessionSummaries.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let props = sessionSummaries[indexPath.row].sessionProps
        let cell = tableView.dequeueReusableCellWithIdentifier("default") as UITableViewCell
        if props.intendedIntensity > 0.7 {
            cell.textLabel!.textColor = UIColor.redColor()
        } else if props.intendedIntensity < 0.4 {
            cell.textLabel!.textColor = UIColor.greenColor()
        }
        
        let dateText = NSDateFormatter.localizedStringFromDate(props.startDate, dateStyle: NSDateFormatterStyle.MediumStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
        cell.textLabel!.text = ", ".join(props.muscleGroupKeys)
        cell.detailTextLabel!.text = "On \(dateText)"
        
        return cell
    }
    

}