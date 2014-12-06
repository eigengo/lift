import Foundation

let isoDateFormatter: NSDateFormatter = {
    let dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    return dateFormatter
}()


extension Exercise.SessionSummary {
    
    /*
return json.arrayValue.map { json -> Exercise.SessionSummary in
let id = NSUUID(UUIDString: json["id"].stringValue)!
let sessionProps = json["sessionProps"]
let startDate = self.isoDateFormatter.dateFromString(sessionProps["startDate"].stringValue)!
let muscleGroupKeys = sessionProps["muscleGroupKeys"].arrayValue.map { $0.stringValue }
let intendedIntensity = sessionProps["intendedIntensity"].doubleValue
let p = Exercise.SessionProps(startDate: startDate, muscleGroupKeys: muscleGroupKeys, intendedIntensity: intendedIntensity)
return Exercise.SessionSummary(id: id, sessionProps: p)
*/

    static func unmarshal(json: JSON) -> Exercise.SessionSummary {
        let sessionProps = json["sessionProps"]
        let id = NSUUID(UUIDString: json["id"].stringValue)!
        return Exercise.SessionSummary(id: id, sessionProps: Exercise.SessionProps.unmarshal(sessionProps))
    }
}

extension Exercise.SessionProps {
    
    static func unmarshal(json: JSON) -> Exercise.SessionProps {
        let startDate = isoDateFormatter.dateFromString(json["startDate"].stringValue)!
        let muscleGroupKeys = json["muscleGroupKeys"].arrayValue.map { $0.stringValue }
        let intendedIntensity = json["intendedIntensity"].doubleValue
        return Exercise.SessionProps(startDate: startDate, muscleGroupKeys: muscleGroupKeys, intendedIntensity: intendedIntensity)
    }
    
}

extension Exercise.Exercise {
    
    static func unmarshal(json: JSON) -> Exercise.Exercise {
        return Exercise.Exercise(name: json["name"].stringValue, intensity: json["intensity"].double)
    }
    
}

extension Exercise.ExerciseSet {
    
    static func unmarshal(json: JSON) -> Exercise.ExerciseSet {
        return Exercise.ExerciseSet(exercises: json["exercises"].arrayValue.map(Exercise.Exercise.unmarshal))
    }
    
}

extension Exercise.ModelMetadata {
    
    static func unmarshal(json: JSON) -> Exercise.ModelMetadata {
        return Exercise.ModelMetadata(version: json["version"].intValue)
    }
    
}

extension Exercise.ExerciseSession {
    
    static func unmarshal(json: JSON) -> Exercise.ExerciseSession {
        let sessionProps = Exercise.SessionProps.unmarshal(json["sessionProps"])
        let sets = json["sets"].arrayValue.map(Exercise.ExerciseSet.unmarshal)
        
        return Exercise.ExerciseSession(sessionProps: sessionProps, sets: sets)
    }
    
}

extension Exercise.MuscleGroup {
    
    static func unmarshal(json: JSON) -> Exercise.MuscleGroup {
        return Exercise.MuscleGroup(
            key: json["key"].stringValue,
            title: json["title"].stringValue,
            exercises: json["exercises"].arrayValue.map { $0.stringValue })
    }
    
}