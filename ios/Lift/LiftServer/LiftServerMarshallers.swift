import Foundation

let isoDateFormatter: NSDateFormatter = {
    let dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    return dateFormatter
}()


extension Exercise.SessionSummary {
    
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

extension Exercise.ModelMetadata {
    
    static func unmarshal(json: JSON) -> Exercise.ModelMetadata {
        return Exercise.ModelMetadata(version: json["version"].intValue)
    }
    
}

extension Exercise.ExerciseSession {
    
    static func unmarshal(json: JSON) -> Exercise.ExerciseSession {
        let sessionProps = Exercise.SessionProps.unmarshal(json["sessionProps"])
        let exercises = json["exercises"].arrayValue.map(Exercise.Exercise.unmarshal)
        let modelMetadata = Exercise.ModelMetadata.unmarshal(json["modelMetadata"])
        
        return Exercise.ExerciseSession(sessionProps: sessionProps, exercises: exercises, modelMetadata: modelMetadata)
    }
    
}