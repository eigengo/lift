import Foundation

class ExerciseSession : NSObject {
    var props: Exercise.SessionProps
    var id: NSUUID
    
    init(id: NSUUID, props: Exercise.SessionProps) {
        self.id = id
        self.props = props
        super.init()
    }
    
}

@objc protocol ExerciseSessionSettable {
    
    func setExerciseSession(session: ExerciseSession)
    
}