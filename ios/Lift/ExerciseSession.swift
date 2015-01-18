import Foundation

/**
 * Models an exercise session. 
 */
class ExerciseSession : NSObject {
    var props: Exercise.SessionProps
    var id: NSUUID
    var isOffline: Bool
    
    /**
     * Constructs a session with the given id, props and indicator whether the session is offline
     * right from the start.
     */
    init(id: NSUUID, props: Exercise.SessionProps, isOffline: Bool) {
        self.id = id
        self.props = props
        self.isOffline = isOffline
        super.init()
    }
    
    /**
     * Submits multi-packet for the current session
     */
    func submitData(mp: MultiPacket) {
        if isOffline {
            NSLog("Offline: Saved MP.")
        }
        LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: id, data: mp) {
            $0.cata({ err in self.isOffline = true}, r: { _ in NSLog("Online: Removed MP."); self.isOffline = false })
        }
    }
    
    /**
     * Ends the current session. After ending, this instance is not re-usable
     */
    func end() {
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: id) { _ in }
    }
    
    /**
     * Obtain classification examples for the given session
     */
    func getClassificationExamples(f: Result<[Exercise.Exercise]> -> Void) -> Void {
        return LiftServer.sharedInstance.exerciseSessionGetClassificationExamples(CurrentLiftUser.userId!, sessionId: id, f)
    }
    
    /** 
     * Start explicit classification of the given exercise
     */
    func startExplicitClassification(exercise: Exercise.Exercise) -> Void {
        LiftServer.sharedInstance.exerciseSessionStartExplicitClassification(CurrentLiftUser.userId!, sessionId: id, exercise: exercise, f: const(()))
    }
    
    /** 
     * End explicit classification
     */
    func endExplicitClassification() {
        LiftServer.sharedInstance.exerciseSessionEndExplicitClassification(CurrentLiftUser.userId!, sessionId: id, f: const(()))
    }
}

/**
 * Allows a session to be set
 */
@objc protocol ExerciseSessionSettable {
    
    /**
     * Typically invoked after a sesion is constructed and ready to be started
     */
    func setExerciseSession(session: ExerciseSession)
    
}
