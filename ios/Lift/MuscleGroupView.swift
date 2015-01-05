import Foundation

extension Exercise.MuscleGroup {
    
    static func titlesFromMuscleGroupKeys(keys: [Exercise.MuscleGroupKey], groups: [Exercise.MuscleGroup]?) -> [String] {
        return muscleGroupsFromMuscleGroupKeys(keys, groups: groups).map { $0.title }
    }
    
    static func muscleGroupsFromMuscleGroupKeys(keys: [Exercise.MuscleGroupKey], groups: [Exercise.MuscleGroup]?) -> [Exercise.MuscleGroup] {
        if let gs = groups {
            return keys.map { key in
                for g in gs {
                    if g.key == key {
                        return g
                    }
                }
                return Exercise.MuscleGroup(key: key, title: key, exercises: [])
            }
        } else {
            return keys.map { key in return Exercise.MuscleGroup(key: key, title: key, exercises: []) }
        }
    }
    

}