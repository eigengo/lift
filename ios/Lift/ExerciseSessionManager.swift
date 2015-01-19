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
    
    func removeAllOfflineSessions() -> Void {
        if let offlineSessions = NSFileManager.defaultManager().contentsOfDirectoryAtPath(documentsPath, error: nil) as? [String] {
            for offlineSession in offlineSessions {
                NSFileManager.defaultManager().removeItemAtPath(documentsPath.stringByAppendingPathComponent(offlineSession), error: nil)
            }
        }
    }
    
    func removeOfflineSession(id: NSUUID) -> Void {
        NSFileManager.defaultManager().removeItemAtPath(documentsPath.stringByAppendingPathComponent(id.UUIDString), error: nil)
    }
    
    func listOfflineSessions(date: NSDate) -> [Exercise.SessionProps] {
        func loadOfflineSession(offlineSessionPath: String) -> Exercise.SessionProps {
            let realPath = documentsPath.stringByAppendingPathComponent(offlineSessionPath)
            return Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        }
        
        if let offlineSessions = NSFileManager.defaultManager().contentsOfDirectoryAtPath(documentsPath, error: nil) as? [String] {
            return offlineSessions.map(loadOfflineSession)
        } else {
            return []
        }
    }
    
    class ManagerManagedExerciseSessionIO : ManagedExerciseSessionIO {
        var allMultiPacketsFileName: String
        var rootPath: String
        
        init(rootPath: String) {
            self.rootPath = rootPath
            allMultiPacketsFileName = rootPath.stringByAppendingPathComponent("all.mp")
            NSFileManager.defaultManager().createFileAtPath(allMultiPacketsFileName, contents: nil, attributes: nil)
        }
        
        func remove() {
            NSFileManager.defaultManager().removeItemAtPath(rootPath, error: nil)
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

/**
 * IO functions for the managed session
 */
protocol ManagedExerciseSessionIO {
    
    func appendMultiPacket(mp: MultiPacket)

    func remove()
    
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
        
        if !isOffline {
            managedSession.submitData(mp) { x in
                x.cata({ _ in self.isOffline = true }, r: { _ in self.isOffline = false })
                f(x)
            }
        } else {
            f(Result.value(()))
        }
    }
    
    override func end(f: Result<Void> -> Void) -> Void {
        if !isOffline {
            managedSession.end { $0.cata(const(()), r: { _ in self.io.remove() }) }
        }
        f(Result.value(()))
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
