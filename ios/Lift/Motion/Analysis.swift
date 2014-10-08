import UIKit

struct Point3d {
    var x = 0.0
    var y = 0.0
    var z = 0.0
}

struct Point3dTime {
    var point: Point3d
    var time: NSDate
}

class MotionAnalyser {
    var items = [Point3dTime]()

     func add(value: Point3dTime) {
        self.items.append(value);
    }
    
}
