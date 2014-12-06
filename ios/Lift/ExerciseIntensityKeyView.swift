import UIKit

extension Exercise.ExerciseIntensityKey {
    /*
    internal struct Colors {
        let veryLight = UIColor.grayColor()
        let light = UIColor(red: 0.333, green: 0.584, blue: 0.902, alpha: 1)
        let moderate = UIColor(red: 0.239, green: 0.635, blue: 0.302, alpha: 1)
        let hard = UIColor(red: 0.914, green: 0.69, blue: 0, alpha: 1)
        let veryHard = UIColor(red: 0.808, green: 0.192, blue: 0.243, alpha: 1)
        let bleedingEyes = UIColor(red: 0.58, green: 0.129, blue: 0.576, alpha: 1)
    }
    */
    
    func textColor() -> UIColor {
        if self <= 0.3 {
            // very light -> #59ABE3
            // was return UIColor.grayColor()
            return UIColor(red: 0.349, green: 0.671, blue: 0.89, alpha: 1)
        } else if self <= 0.45 {
            // light -> 3498DB (was #5595E6)
            // was return UIColor(red: 0.333, green: 0.584, blue: 0.902, alpha: 1)
            return UIColor(red: 0.204, green: 0.596, blue: 0.859, alpha: 1)
        } else if self <= 0.65 {
            // moderate -> 4183D7 (was #3DA24D)
            // was return UIColor(red: 0.239, green: 0.635, blue: 0.302, alpha: 1)
            return UIColor(red: 0.255, green: 0.514, blue: 0.843, alpha: 1)
        } else if self <= 0.75 {
            // hard -> 446CB3 (was #E9B000)
            // was return UIColor(red: 0.914, green: 0.69, blue: 0, alpha: 1)
            return UIColor(red: 0.267, green: 0.424, blue: 0.702, alpha: 1)
        } else if self <= 0.87 {
            // very hard -> 1F3A93 (was #CE313E)
            // was return UIColor(red: 0.808, green: 0.192, blue: 0.243, alpha: 1)
            return UIColor(red: 0.122, green: 0.227, blue: 0.576, alpha: 1)
        }
        // bleeding eyes -> 22313F (was #942193)
        // was return UIColor(red: 0.58, green: 0.129, blue: 0.576, alpha: 1)
        return UIColor(red: 0.133, green: 0.192, blue: 0.247, alpha: 1)
    }
    
}