import Foundation

extension Exercise.MuscleGroup {
    
    static func titlesFromMuscleGroupKeys(keys: [Exercise.MuscleGroupKey], groups: [Exercise.MuscleGroup]?) -> [String] {
        if let gs = groups {
            return keys.map { key in
                for g in gs {
                    if g.key == key {
                        return g.title
                    }
                }
                return key
            }
        } else {
            return keys
        }
    }
    
}