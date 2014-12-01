import Foundation

struct User {
    var uuid: NSUUID
}

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
    case UserRegisterDevice(userId: NSUUID)
    
    ///
    /// Retrieves the user's profile for the ``userId``
    ///
    case UserGetProfile(userId: NSUUID)

    ///
    /// Retrieves all the exercises for the given ``userId``
    ///
    case ExerciseGetAllExercises(userId: NSUUID)
    
    ///
    /// Starts an exercise session for the given ``userId``
    ///
    case ExerciseSessionStart(userId: NSUUID)
    
    ///
    /// Submits the data (received from the smartwatch) for the given ``userId``, ``sessionId``
    ///
    case ExerciseSessionSubmitData(userId: NSUUID, sessionId: NSUUID)
    
    ///
    /// Ends the session for the given ``userId`` and ``sessionId``
    ///
    case ExerciseSessionEnd(userId: NSUUID, sessionId: NSUUID)
    
    // MARK: URLStringConvertible
    var Request: LiftServerRequest {
        get {
            let r: LiftServerRequest = {
                switch self {
                case let .UserRegister: return LiftServerRequest(path: "/user", method: Method.POST)
                case let .UserLogin: return LiftServerRequest(path: "/user", method: Method.PUT)
                    
                case let .UserRegisterDevice(userId): return LiftServerRequest(path: "/user/\(userId)/device/ios", method: Method.POST)
                case let .UserGetProfile(userId): return LiftServerRequest(path: "/user/\(userId)", method: Method.GET)
                    
                case let .ExerciseGetAllExercises(userId): return LiftServerRequest(path: "/exercise/\(userId)", method: Method.GET)
                
                case let .ExerciseSessionStart(userId): return LiftServerRequest(path: "/exercise/\(userId)", method: Method.POST)
                case let .ExerciseSessionSubmitData(userId, sessionId): return LiftServerRequest(path: "/exercise/\(userId)/\(sessionId)", method: Method.PUT)
                case let .ExerciseSessionEnd(userId, sessionId): return LiftServerRequest(path: "/exercise/\(userId)/\(sessionId)", method: Method.DELETE)
                }
            }()
            
            return r
        }
    }
}

///
/// Lift server connection 
///
public class LiftServer {
    
    public class var sharedInstance: LiftServer {
        struct Singleton {
            static let instance = LiftServer()
        }
        
        return Singleton.instance
    }

    private let manager = Manager(configuration: {
        var configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
        
        configuration.HTTPAdditionalHeaders = {
            // Accept-Encoding HTTP Header; see http://tools.ietf.org/html/rfc7230#section-4.2.3
            let acceptEncoding: String = "gzip;q=1.0,compress;q=0.5"
            
            // Accept-Language HTTP Header; see http://tools.ietf.org/html/rfc7231#section-5.3.5
            let acceptLanguage: String = {
                var components: [String] = []
                for (index, languageCode) in enumerate(NSLocale.preferredLanguages() as [String]) {
                    let q = 1.0 - (Double(index) * 0.1)
                    components.append("\(languageCode);q=\(q)")
                    if q <= 0.5 {
                        break
                    }
                }
                
                return join(",", components)
                }()
            
            // User-Agent Header; see http://tools.ietf.org/html/rfc7231#section-5.5.3
            let userAgent: String = "org.eigengo.Lift (iOS)"
            
            return ["Accept-Encoding": acceptEncoding,
                "Accept-Language": acceptLanguage,
                "User-Agent": userAgent]
            }()
        
        return configuration
        }()
    )
    private let baseURLString = "http://192.168.101.102:12552"
    
    private func error(code: Int) -> NSError {
        return NSError(domain: "com.eigengo.lift", code: code, userInfo: nil)
    }
    

    ///
    /// Body is either JSON structure or NSData
    ///
    private enum Body {
        case Json(params: [String : AnyObject])
        case Data(data: NSData)
    }

    ///
    /// Make a request to the Lift server
    ///
    private func liftRequest(req: LiftServerRequestConvertible, body: Body? = nil) -> Request {
        let lsr = req.Request
        switch body {
        case let .Some(Body.Json(params)): return request(lsr.method, baseURLString + lsr.path, parameters: params, encoding: ParameterEncoding.JSON)
        case let .Some(Body.Data(data)): return upload(lsr.method, baseURLString + lsr.path, data)
        case .None: return request(lsr.method, baseURLString + lsr.path, parameters: nil, encoding: ParameterEncoding.URL)
        }
    }
    
    func login<U>(email: String, password: String, f: Result<User> -> U) -> Void {
        liftRequest(LiftServerURLs.UserLogin(), body: .Json(params: [ "email": email, "password": password ]))
            .responseString { (_, _, body, error) in
                if error != nil {
                    f(Result.error(error!))
                } else {
                    let userId = NSUUID(UUIDString: body!)
                    f(Result.value(User(uuid: userId!)))
                }
            }
    }
    
    func register<U>(email: String, password: String, f: Result<User> -> U) -> Void {
        liftRequest(LiftServerURLs.UserRegister(), body: .Json(params: [ "email": email, "password": password ]))
            .responseString { (_, _, body, error) -> Void in
                if error != nil {
                    f(Result.error(error!))
                } else {
                    let userId = NSUUID(UUIDString: body!)
                    f(Result.value(User(uuid: userId!)))
                }
        }

        
    }
    
}
