import Foundation
/*
class ExerciseSession : NSObject {
 
    /**
     * Submits multi-packet for the current session
     */
    func submitData(mp: MultiPacket) -> Void { fatalError("Not implemented") }
    
    /**
     * Ends the current session. After ending, this instance is not re-usable
     */
    func end() -> Void { fatalError("Not implemented") }
    
    /**
     * Obtain classification examples for the given session
     */
    func getClassificationExamples(f: Result<[Exercise.Exercise]> -> Void) -> Void { fatalError("Not implemented") }
    
    /**
     * Start explicit classification of the given exercise
     */
    func startExplicitClassification(exercise: Exercise.Exercise) -> Void { fatalError("Not implemented") }
    
    /**
     * End explicit classification
     */
    func endExplicitClassification() -> Void { fatalError("Not implemented") }
}
*/
/**
 * Models an exercise session. 
 */
class ExerciseSession : NSObject {
    internal var props: Exercise.SessionProps
    internal var id: NSUUID
    
    /**
     * Constructs a session with the given id, props and indicator whether the session is offline
     * right from the start.
     */
    init(id: NSUUID, props: Exercise.SessionProps) {
        self.id = id
        self.props = props
        super.init()
    }
    
    /**
     * Submits multi-packet for the current session
     */
    func submitData(mp: MultiPacket, f: Result<Void> -> Void) -> Void {
        LiftServer.sharedInstance.exerciseSessionSubmitData(CurrentLiftUser.userId!, sessionId: id, data: mp, f: f)
    }
    
    /**
     * Ends the current session. After ending, this instance is not re-usable
     */
    func end(f: Result<Void> -> Void) -> Void {
        LiftServer.sharedInstance.exerciseSessionEnd(CurrentLiftUser.userId!, sessionId: id, f: f)
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
