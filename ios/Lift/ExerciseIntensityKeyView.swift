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
            // very light
            return UIColor.grayColor()
        } else if self <= 0.45 {
            // light -> #5595E6
            return UIColor(red: 0.333, green: 0.584, blue: 0.902, alpha: 1)
        } else if self <= 0.65 {
            // moderate -> #3DA24D
            return UIColor(red: 0.239, green: 0.635, blue: 0.302, alpha: 1)
        } else if self <= 0.75 {
            // hard -> #E9B000
            return UIColor(red: 0.914, green: 0.69, blue: 0, alpha: 1)
        } else if self <= 0.87 {
            // very hard -> #CE313E
            return UIColor(red: 0.808, green: 0.192, blue: 0.243, alpha: 1)
        }
        // bleeding eyes -> #942193
        return UIColor(red: 0.58, green: 0.129, blue: 0.576, alpha: 1)
    }
    
}