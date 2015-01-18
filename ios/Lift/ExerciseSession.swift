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
    
}

@objc protocol ExerciseSessionSettable {
    
    func setExerciseSession(session: ExerciseSession)
    
}