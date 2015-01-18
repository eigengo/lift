import Foundation

class ExerciseSession : NSObject {
    var props: Exercise.SessionProps
    var id: NSUUID
    var isOffline: Bool
    
    init(id: NSUUID, props: Exercise.SessionProps, isOffline: Bool) {
        self.id = id
        self.props = props
        self.isOffline = isOffline
        super.init()
    }
    
    func submitData(mp: MultiPacket) {
        if isOffline {
            NSLog("Offline: Saved MP.")
        }
        LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: id, data: mp) {
            $0.cata({ err in self.isOffline = true}, r: { _ in NSLog("Online: Removed MP."); self.isOffline = false })
        }
    }
    
    func end() {
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: id) { _ in }
    }
    
    func getClassificationExamples(f: Result<[Exercise.Exercise]> -> Void) -> Void {
        return LiftServer.sharedInstance.exerciseSessionGetClassificationExamples(CurrentLiftUser.userId!, sessionId: id, f)
    }
    
    func startExplicitClassification(exercise: Exercise.Exercise) -> Void {
        LiftServer.sharedInstance.exerciseSessionStartExplicitClassification(CurrentLiftUser.userId!, sessionId: id, exercise: exercise, f: const(()))
    }
    
    func endExplicitClassification() {
        LiftServer.sharedInstance.exerciseSessionEndExplicitClassification(CurrentLiftUser.userId!, sessionId: id, f: const(()))
    }
}

@objc protocol ExerciseSessionSettable {
    
    func setExerciseSession(session: ExerciseSession)
    
}