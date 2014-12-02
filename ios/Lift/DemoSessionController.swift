import Foundation

class DemoSessionController : UIViewController, MuscleGroupsSettable {
    
    func setMuscleGroups(muscleGroups: [String]) {
        NSLog("Starting with %@", muscleGroups)
    }

}