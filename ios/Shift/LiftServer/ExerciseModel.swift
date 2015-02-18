import Foundation

struct Exercise {
    
    ///
    /// MSG alias
    ///
    typealias MuscleGroupKey = String
    
    ///
    /// Exercise intensity value
    ///
    typealias ExerciseIntensityKey = Double
    
    ///
    /// Default exercise intensities
    ///
    static let defaultIntensities = [
        ExerciseIntensity.veryLight,
        ExerciseIntensity.light,
        ExerciseIntensity.moderate,
        ExerciseIntensity.hard,
        ExerciseIntensity.veryHard,
        ExerciseIntensity.brutal
    ]
    
    ///
    /// Single muscle group cell holding the key, which is the communication key with the server
    /// together with the (localisable) title and set of exercises
    ///
    struct MuscleGroup {
        /// the key that identifies the muscle group to the server
        var key: MuscleGroupKey
        /// the title—in the future received from the server (localised)
        var title: String
        /// the sample/suggested exercises—in the future received from the server (localised)
        var exercises: [String]
    }
    
    ///
    /// Session properties
    ///
    struct SessionProps {
        /// the start date
        var startDate: NSDate
        /// the targeted muscle groups
        var muscleGroupKeys: [String]
        /// the intended intensity
        var intendedIntensity: ExerciseIntensityKey
    }
    
    /// Metric
    
    struct Metric {
        // value
        // metricUnit
        var value: Double
        var metricUnit: String //Mass or Distance
    }
    
    ///
    /// Sigle exercise
    ///
    struct Exercise {
        var name: String
        var intensity: ExerciseIntensityKey?
        var metric: Metric?
    }
    
    ///
    /// Exercise set
    ///
    struct ExerciseSet {
        var exercises: [Exercise]
    }

    ///
    /// Exercise intensity
    ///
    struct ExerciseIntensity {
        var intensity: ExerciseIntensityKey
        var title: String
        var userNotes: String
        
        /// Default intensities
        static let veryLight = ExerciseIntensity(intensity: 0.30, title: "VeryLight".localized(), userNotes: "VeryLightUserNotes".localized())
        static let light     = ExerciseIntensity(intensity: 0.45, title: "Light".localized(),     userNotes: "LightUserNotes".localized())
        static let moderate  = ExerciseIntensity(intensity: 0.65, title: "Moderate".localized(),  userNotes: "ModerateUserNotes".localized())
        static let hard      = ExerciseIntensity(intensity: 0.75, title: "Hard".localized(),      userNotes: "HardUserNotes".localized())
        static let veryHard  = ExerciseIntensity(intensity: 0.87, title: "VeryHard".localized(),  userNotes: "VeryHardUserNotes".localized())
        static let brutal    = ExerciseIntensity(intensity: 0.95, title: "Brutal".localized(),    userNotes: "BrutalUserNotes".localized())
    }
    
    ///
    /// Classification model metadata
    ///
    struct ModelMetadata {
        var version: Int
    }
    
    ///
    /// Session intensity
    ///
    struct SessionIntensity {
        var intended: ExerciseIntensityKey
        var actual: ExerciseIntensityKey
    }
    
    ///
    /// Session date summary
    ///
    struct SessionDate {
        var date: NSDate
        var sessionIntensities: [SessionIntensity]
    }
    
    ///
    /// Session summary model
    ///
    struct SessionSummary {
        var id: NSUUID
        var sessionProps: SessionProps
        var setIntensities: [ExerciseIntensityKey]
    }
    
    ///
    /// Session suggestions
    ///
    struct SessionSuggestion {
        /// the target muscle groups
        var muscleGroupKeys: [String]
        /// the intended intensity
        var intendedIntensity: ExerciseIntensityKey
    }
    
    ///
    /// Association of list of exercise with a particular session props
    ///
    struct ExerciseSession {
        var sessionProps: SessionProps
        var sets: [ExerciseSet]
    }
    
}
