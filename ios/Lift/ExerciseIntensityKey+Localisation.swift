import Foundation

extension Exercise.ExerciseIntensityKey {
    
    ///
    /// Returns the closest matching ExerciseIntensity for this value
    ///
    var intensity: Exercise.ExerciseIntensity {
        get {
            if self <= Exercise.ExerciseIntensity.veryLight.intensity { return Exercise.ExerciseIntensity.veryLight }
            if self <= Exercise.ExerciseIntensity.light.intensity { return Exercise.ExerciseIntensity.light }
            if self <= Exercise.ExerciseIntensity.moderate.intensity { return Exercise.ExerciseIntensity.moderate }
            if self <= Exercise.ExerciseIntensity.hard.intensity { return Exercise.ExerciseIntensity.hard }
            if self <= Exercise.ExerciseIntensity.veryHard.intensity { return Exercise.ExerciseIntensity.veryHard }
            return Exercise.ExerciseIntensity.brutal
        }
    }
    
}