import Foundation

struct OfflineExerciseSession {
    var id: NSUUID
    var offlineFromStart: Bool
    var props: Exercise.SessionProps
}

extension OfflineExerciseSession {
    
    static func unmarshal(json: JSON) -> OfflineExerciseSession {
        let id = NSUUID(UUIDString: json["id"].stringValue)!
        let offlineFromStart = json["offlineFromStart"].boolValue
        let props = Exercise.SessionProps.unmarshal(json["props"])
        return OfflineExerciseSession(id: id, offlineFromStart: offlineFromStart, props: props)
    }
    
    func marshal() -> [String : AnyObject] {
        var params: [String : AnyObject] = [:]
        params["id"] = id.UUIDString
        params["offlineFromStart"] = offlineFromStart
        params["props"] = props.marshal()
        return params
    }
    
}

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

        var error: NSError?
        let x = OfflineExerciseSession(id: managedSession.id, offlineFromStart: isOfflineFromStart, props: managedSession.props).marshal()
        let os = NSOutputStream(toFileAtPath: rootPath.stringByAppendingPathComponent("props.json"), append: false)!
        os.open()
        NSJSONSerialization.writeJSONObject(x, toStream: os, options: NSJSONWritingOptions.PrettyPrinted, error: &error)
        os.close()
        
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
    
    func listOfflineSessions() -> [OfflineExerciseSession] {
        
        func loadOfflineSession(offlineSessionPath: String) -> [OfflineExerciseSession] {
            let rootPath = documentsPath.stringByAppendingPathComponent(offlineSessionPath)
            let propsJsonPath = rootPath.stringByAppendingPathComponent("props.json")
            if let propsJsonData = NSData(contentsOfFile: propsJsonPath) {
                let json = JSON(data: propsJsonData, options: NSJSONReadingOptions.AllowFragments, error: nil)
                return [OfflineExerciseSession.unmarshal(json)]
            }
            return []
        }
        
        var result: [OfflineExerciseSession] = []
        if let offlineSessions = NSFileManager.defaultManager().contentsOfDirectoryAtPath(documentsPath, error: nil) as? [String] {
            for offlineSession in offlineSessions {
                result += loadOfflineSession(offlineSession)
            }
        }
        
        return result
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
            NSLog("Removed %@", rootPath)
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
                x.cata({ _ in self.isOffline = true }, r: const(()))
                f(x)
            }
        } else {
            f(Result.value(()))
        }
    }
    
    override func end(f: Result<Void> -> Void) -> Void {
        if !isOffline {
            managedSession.end { x in
                x.cata(const(()), r: { _ in self.io.remove() })
                f(x)
            }
        }
        f(Result.value(()))
    }
    
    override func getClassificationExamples(f: Result<[Exercise.Exercise]> -> Void) -> Void {
        managedSession.getClassificationExamples(f)
    }
    
    override func startExplicitClassification(exercise: Exercise.Exercise) -> Void {
        if !isOffline {
            managedSession.startExplicitClassification(exercise)
        }
    }
    
    override func endExplicitClassification() -> Void {
        if !isOffline {
            managedSession.endExplicitClassification()
        }
    }
    
}
