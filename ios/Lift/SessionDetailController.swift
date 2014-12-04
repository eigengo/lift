import Foundation

class SessionDetailController : UIViewController, UITableViewDataSource {
    @IBOutlet var tableView: UITableView!
    private var exerciseSession: Exercise.ExerciseSession?
    
    func setExerciseSession(exerciseSession: Exercise.ExerciseSession) {
        self.exerciseSession = exerciseSession
        self.tableView.reloadData()
    }
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if exerciseSession == nil {
            return 0
        }

        if section == 0 { return 1 }
        return exerciseSession!.exercises.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        
        func exerciseCell(row: Int) -> UITableViewCell {
            let exercise = exerciseSession!.exercises[row]
            let cell = tableView.dequeueReusableCellWithIdentifier("exercise") as UITableViewCell
            cell.textLabel!.text = exercise.name
            if exercise.intensity != nil { cell.textLabel!.textColor = exercise.intensity!.textColor() }
            
            return cell
        }
        
        func summaryCell() -> UITableViewCell {
            let props = exerciseSession!.sessionProps
            let metadata = exerciseSession!.modelMetadata
            let cell = tableView.dequeueReusableCellWithIdentifier("session") as UITableViewCell

            cell.textLabel!.text = ", ".join(props.muscleGroupKeys)
            cell.textLabel!.textColor = props.intendedIntensity.textColor()
            let dateText = NSDateFormatter.localizedStringFromDate(props.startDate, dateStyle: NSDateFormatterStyle.MediumStyle, timeStyle: NSDateFormatterStyle.MediumStyle)
            cell.detailTextLabel!.text = "On \(dateText) classified with \(metadata.version)"
            
            return cell
        }
        
        
        if indexPath.section == 0 { return summaryCell() }
        return exerciseCell(indexPath.row)
    }
    
}