import Foundation

class DemoSessionController : UIViewController, UITabBarDelegate, MuscleGroupsSettable {
    
    func setMuscleGroupKeys(muscleGroupKeys: [String]) {
        NSLog("Starting with %@", muscleGroupKeys)
    }
    
    func tabBar(tabBar: UITabBar, didSelectItem item: UITabBarItem!) {
        // we only have one item, so back we go
        performSegueWithIdentifier("end", sender: nil)
    }
    
}