import Foundation

/**
 * Offline manager maintains a list of offline sessions.
 */
class ExerciseSessionManager {
    
    ///
    /// Singleton instance of the LiftServer. The instances are stateless, so it is generally a
    /// good idea to take advantage of the singleton
    ///
    class var sharedInstance: ExerciseSessionManager {
        struct Singleton {
            static let instance = ExerciseSessionManager()
        }
        
        return Singleton.instance
    }
    
    private let documentsPath = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true).first as String

    /**
     * Wraps a session in the manager, which allows it to be safely recorded in case the connection to the
     * Lift server drops or becomes unreliable
     *
     * @param managedSession the session to be managed
     * @param isOfflineFromStart true if the session was not started with the real ``id`` from the server
     */
    func managedSession(managedSession: ExerciseSession, isOfflineFromStart: Bool) -> ManagedExerciseSession {
        let rootPath = documentsPath.stringByAppendingPathComponent(managedSession.id.UUIDString)
        NSFileManager.defaultManager().createDirectoryAtPath(rootPath, withIntermediateDirectories: false, attributes: nil, error: nil)
        let io = ManagerManagedExerciseSessionIO(rootPath: rootPath)
        
        return ManagedExerciseSession(io: io, managedSession: managedSession, isOfflineFromStart: isOfflineFromStart)
    }

    // TODO: functions that get / delete offline sessions
    
    class ManagerManagedExerciseSessionIO : ManagedExerciseSessionIO {
        var allMultiPacketsFileName: String
        
        init(rootPath: String) {
            allMultiPacketsFileName = rootPath.stringByAppendingPathComponent("all.mp")
            NSFileManager.defaultManager().createFileAtPath(allMultiPacketsFileName, contents: nil, attributes: nil)
        }
        
        func appendMultiPacket(mp: MultiPacket) {
            let handle = NSFileHandle(forWritingAtPath: allMultiPacketsFileName)!
            handle.seekToEndOfFile()
            handle.writeData(mp.data())
            handle.closeFile()
            
            NSLog("Written to %@", allMultiPacketsFileName)
        }
        
    }
    
}

protocol ManagedExerciseSessionIO {
    
    func appendMultiPacket(mp: MultiPacket)
    
}

/**
 * Wraps an existing session in the file management operations
 */
class ManagedExerciseSession : ExerciseSession {
    var managedSession: ExerciseSession
    var isOffline: Bool
    var isOfflineFromStart: Bool
    var io: ManagedExerciseSessionIO
    
    init(io: ManagedExerciseSessionIO, managedSession: ExerciseSession, isOfflineFromStart: Bool) {
        self.managedSession = managedSession
        self.isOffline = isOfflineFromStart
        self.isOfflineFromStart = isOfflineFromStart
        self.io = io

        super.init(id: managedSession.id, props: managedSession.props)
    }
    
    override func submitData(mp: MultiPacket, f: Result<Void> -> Void) -> Void {
        io.appendMultiPacket(mp)
        
        managedSession.submitData(mp) { x in
            x.cata({ _ in self.isOffline = true }, r: { _ in self.isOffline = false })
        }
    }
    
    override func end() -> Void {
        managedSession.end()
    }
    
    override func getClassificationExamples(f: Result<[Exercise.Exercise]> -> Void) -> Void {
        managedSession.getClassificationExamples(f)
    }
    
    override func startExplicitClassification(exercise: Exercise.Exercise) -> Void {
        managedSession.startExplicitClassification(exercise)
    }
    
    override func endExplicitClassification() -> Void {
        managedSession.endExplicitClassification()
    }
    
}
