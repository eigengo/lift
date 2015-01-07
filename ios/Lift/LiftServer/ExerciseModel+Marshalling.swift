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
        let setIntensities = json["setIntensities"].arrayValue.map { $0.doubleValue }
        return Exercise.SessionSummary(id: id, sessionProps: Exercise.SessionProps.unmarshal(sessionProps), setIntensities: setIntensities)
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

extension Exercise.SessionIntensity {

    static func unmarshal(json: JSON) -> Exercise.SessionIntensity {
        let intended = json["intended"].doubleValue
        let actual = json["actual"].doubleValue
        return Exercise.SessionIntensity(intended: intended, actual: actual)
    }

}

extension Exercise.SessionDate {
    
    static func unmarshal(json: JSON) -> Exercise.SessionDate {
        let date = isoDateFormatter.dateFromString(json["date"].stringValue)!
        return Exercise.SessionDate(date: date, sessionIntensities: json["sessionIntensities"].arrayValue.map(Exercise.SessionIntensity.unmarshal))
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