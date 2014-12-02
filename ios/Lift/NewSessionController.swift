import Foundation

class NewSessionController : UIViewController {
    @IBOutlet
    var demoMode: UISwitch!
    private var muscleGroups: [String]?
    
    override func viewDidLoad() {
        setBackgroundImage(["Start1", "Start2"])
    }
 
    @IBAction
    func startSession(sender: UIButton) {
        let segueName = demoMode.on ? "newsession_demo" : "newsession_live"
        self.muscleGroups = [sender.titleLabel!.text!]
        performSegueWithIdentifier(segueName, sender: nil)
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if let ctrl = segue.destinationViewController as? MuscleGroupsSettable {
            ctrl.setMuscleGroups(self.muscleGroups!)
        }
    }
    
}