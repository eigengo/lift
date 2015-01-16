import Foundation

class SessionDetailController : UIViewController, UITableViewDataSource {
    @IBOutlet var tableView: UITableView!
    private var exerciseSession: Exercise.ExerciseSession?
    
    func setExerciseSession(exerciseSession: Exercise.ExerciseSession) {
        self.exerciseSession = exerciseSession
        self.tableView.reloadData()
    }
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        if exerciseSession == nil {
            return 0
        }
        return exerciseSession!.sets.count + 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if exerciseSession == nil {
            return 0
        }

        if section == 0 { return 1 }
        return exerciseSession!.sets[section - 1].exercises.count
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 0 {
            return "Summary"
        } else {
            return "Set \(section)"
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        
        func exerciseCell(set: Int, row: Int) -> UITableViewCell {
            let exercise = exerciseSession!.sets[set].exercises[row]
            let cell = tableView.dequeueReusableCellWithIdentifier("exercise") as UITableViewCell
            cell.textLabel!.text = exercise.name
            if exercise.intensity != nil { cell.textLabel!.textColor = exercise.intensity!.textColor() }
            
            return cell
        }
        
        func summaryCell() -> UITableViewCell {
            let props = exerciseSession!.sessionProps
            let cell = tableView.dequeueReusableCellWithIdentifier("session") as UITableViewCell

            let mgs = Exercise.MuscleGroup.titlesFromMuscleGroupKeys(props.muscleGroupKeys, groups: LiftServerCache.sharedInstance.exerciseGetMuscleGroups())
            cell.textLabel!.text = ", ".join(mgs)
            cell.textLabel!.textColor = props.intendedIntensity.textColor()
            let dateText = NSDateFormatter.localizedStringFromDate(props.startDate, dateStyle: NSDateFormatterStyle.MediumStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
            cell.detailTextLabel!.text = "On \(dateText)"
            
            return cell
        }
        
        
        if indexPath.section == 0 { return summaryCell() }
        return exerciseCell(indexPath.section - 1, indexPath.row)
    }
    
    // MARK: Actions
    @IBAction
    func share() {
        let textToShare = "Great session!"
        let myWebsite = NSURL(string: "http://lift.eigengo.com/")!
        let items: [AnyObject] = [textToShare, myWebsite]
        let activityViewController = UIActivityViewController(activityItems: items, applicationActivities: nil)
        navigationController?.presentViewController(activityViewController, animated: true, completion: nil)
    }
}