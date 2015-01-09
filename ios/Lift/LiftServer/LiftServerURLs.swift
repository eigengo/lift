import Foundation

///
/// The request to the Lift server-side code
///
struct LiftServerRequest {
    var path: String
    var method: Method
    
    init(path: String, method: Method) {
        self.path = path
        self.method = method
    }
    
}

///
/// Defines mechanism to convert a request to LiftServerRequest
///
protocol LiftServerRequestConvertible {
    var Request: LiftServerRequest { get }
}

///
/// The Lift server URLs and request data mappers
///
enum LiftServerURLs : LiftServerRequestConvertible {
    
    ///
    /// Register the user
    ///
    case UserRegister()
    
    ///
    /// Login the user
    ///
    case UserLogin()
    
    ///
    /// Adds an iOS device for the user identified by ``userId``
    ///
    case UserRegisterDevice(/*userId: */NSUUID)
    
    ///
    /// Retrieves the user's profile for the ``userId``
    ///
    case UserGetPublicProfile(/*userId: */NSUUID)
    
    ///
    /// Sets the user's profile for the ``userId``
    ///
    case UserSetPublicProfile(/*userId: */NSUUID)
    
    ///
    /// Gets the user's profile image
    ///
    case UserGetProfileImage(/*userId: */NSUUID)
    
    ///
    /// Sets the user's profile image
    ///
    case UserSetProfileImage(/*userId: */NSUUID)
    
    ///
    /// Checks that the account is still there
    ///
    case UserCheckAccount(/*userId: */NSUUID)
    
    ///
    /// Get supported muscle groups
    ///
    case ExerciseGetMuscleGroups()
    
    ///
    /// Retrieves all the exercises for the given ``userId`` and ``date``
    ///
    case ExerciseGetExerciseSessionsSummary(/*userId: */NSUUID, /*date: */NSDate)

    ///
    /// Retrieves all the session dates for the given ``userId``
    ///
    case ExerciseGetExerciseSessionsDates(/*userId: */NSUUID)

    ///
    /// Retrieves all the exercises for the given ``userId`` and ``sessionId``
    ///
    case ExerciseGetExerciseSession(/*userId: */NSUUID, /*sessionId: */NSUUID)

    ///
    /// Deletes all the exercises for the given ``userId`` and ``sessionId``
    ///
    case ExerciseDeleteExerciseSession(/*userId: */NSUUID, /*sessionId: */NSUUID)
    
    ///
    /// Starts an exercise session for the given ``userId``
    ///
    case ExerciseSessionStart(/*userId: */NSUUID)
    
    ///
    /// Submits the data (received from the smartwatch) for the given ``userId``, ``sessionId``
    ///
    case ExerciseSessionSubmitData(/*userId: */NSUUID, /*sessionId: */NSUUID)
    
    ///
    /// Ends the session for the given ``userId`` and ``sessionId``
    ///
    case ExerciseSessionEnd(/*userId: */NSUUID, /*sessionId: */NSUUID)
    
    private struct Format {
        private static let simpleDateFormatter: NSDateFormatter = {
            let dateFormatter = NSDateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd"
            return dateFormatter
        }()
        
        static func simpleDate(date: NSDate) -> String {
            return simpleDateFormatter.stringFromDate(date)
        }
        
    }
    
    // MARK: URLStringConvertible
    var Request: LiftServerRequest {
        get {
            let r: LiftServerRequest = {
                switch self {
                case let .UserRegister: return LiftServerRequest(path: "/user", method: Method.POST)
                case let .UserLogin: return LiftServerRequest(path: "/user", method: Method.PUT)
                    
                case .UserRegisterDevice(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)/device/ios", method: Method.POST)
                case .UserGetPublicProfile(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)", method: Method.GET)
                case .UserSetPublicProfile(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)", method: Method.POST)
                case .UserCheckAccount(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)/check", method: Method.GET)
                case .UserGetProfileImage(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)/image", method: Method.GET)
                case .UserSetProfileImage(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)/image", method: Method.POST)
                    
                case .ExerciseGetMuscleGroups(): return LiftServerRequest(path: "/exercise/musclegroups", method: Method.GET)
                    
                case .ExerciseGetExerciseSessionsSummary(let userId, let date): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)?date=\(Format.simpleDate(date))", method: Method.GET)
                case .ExerciseGetExerciseSessionsDates(let userId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)", method: Method.GET)
                case .ExerciseGetExerciseSession(let userId, let sessionId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/\(sessionId.UUIDString)", method: Method.GET)
                case .ExerciseDeleteExerciseSession(let userId, let sessionId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/\(sessionId.UUIDString)", method: Method.DELETE)
                    
                case .ExerciseSessionStart(let userId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/start", method: Method.POST)
                case .ExerciseSessionSubmitData(let userId, let sessionId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/\(sessionId.UUIDString)", method: Method.PUT)
                case .ExerciseSessionEnd(let userId, let sessionId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/\(sessionId.UUIDString)/end", method: Method.POST)
                }
                }()
            
            return r
        }
    }
}

