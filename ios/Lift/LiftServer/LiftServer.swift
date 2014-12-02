import Foundation

struct User {
    var id: NSUUID
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
    case UserRegisterDevice(/*userId: */NSUUID)
    
    ///
    /// Retrieves the user's profile for the ``userId``
    ///
    case UserGetProfile(/*userId: */NSUUID)

    ///
    /// Retrieves all the exercises for the given ``userId``
    ///
    case ExerciseGetAllExercises(/*userId: */NSUUID)
    
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
    
    // MARK: URLStringConvertible
    var Request: LiftServerRequest {
        get {
            let r: LiftServerRequest = {
                switch self {
                case let .UserRegister: return LiftServerRequest(path: "/user", method: Method.POST)
                case let .UserLogin: return LiftServerRequest(path: "/user", method: Method.PUT)
                    
                case .UserRegisterDevice(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)/device/ios", method: Method.POST)
                case .UserGetProfile(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)", method: Method.GET)
                    
                case .ExerciseGetAllExercises(let userId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)", method: Method.GET)
                
                case .ExerciseSessionStart(let userId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)", method: Method.POST)
                case .ExerciseSessionSubmitData(let userId, let sessionId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/\(sessionId.UUIDString)", method: Method.PUT)
                case .ExerciseSessionEnd(let userId, let sessionId): return LiftServerRequest(path: "/exercise/\(userId.UUIDString)/\(sessionId.UUIDString)", method: Method.DELETE)
                }
            }()
            
            return r
        }
    }
}

///
/// Adds the response negotiation
///
extension Request {
    
    public func responseAsResutlt<A, U>(f: Result<A> -> U, completionHandler: (JSON) -> A) -> Void {
        responseSwiftyJSON { (_, response, json, error) -> Void in
            if error != nil {
                f(Result.error(error!))
            } else if response != nil {
                if response!.statusCode != 200 {
                    let userInfo = [NSLocalizedDescriptionKey : json.stringValue]
                    let err = NSError(domain: "com.eigengo.lift", code: response!.statusCode, userInfo: userInfo)
                    f(Result.error(err))
                } else {
                    let val = completionHandler(json)
                    f(Result.value(val))
                }
            }
        }
    }
    
}

///
/// Lift server connection
///
public class LiftServer {
    
    ///
    /// Singleton instance of the LiftServer. The instances are stateless, so it is generally a 
    /// good idea to take advantage of the singleton
    ///
    public class var sharedInstance: LiftServer {
        struct Singleton {
            static let instance = LiftServer()
        }
        
        return Singleton.instance
    }

    ///
    /// The connection manager's configuration
    ///
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
    private let baseURLString = "http://192.168.59.103:8081"
    
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
    private func request(req: LiftServerRequestConvertible, body: Body? = nil) -> Request {
        let lsr = req.Request
        switch body {
        case let .Some(Body.Json(params)): return manager.request(lsr.method, baseURLString + lsr.path, parameters: params, encoding: ParameterEncoding.JSON)
        case let .Some(Body.Data(data)): return upload(lsr.method, baseURLString + lsr.path, data)
        case .None: return manager.request(lsr.method, baseURLString + lsr.path, parameters: nil, encoding: ParameterEncoding.URL)
        }
    }
    
    ///
    /// Register the iOS push device token for the given user
    ///
    func registerDeviceToken(userId: NSUUID, deviceToken: NSData) -> Void {
        let tokenString = NSString(data: deviceToken, encoding: NSASCIIStringEncoding)!
        request(LiftServerURLs.UserRegisterDevice(userId), body: .Json(params: [ "deviceToken": tokenString ]))
            .responseString { (_, _, body, error) -> Void in
                println(body)
            }
        
    }
    
    ///
    /// Login the user given the username and password
    ///
    func login(email: String, password: String, f: Result<User> -> Void) -> Void {
        request(LiftServerURLs.UserLogin(), body: .Json(params: [ "email": email, "password": password ]))
            .responseAsResutlt(f) { json -> User in
                let userId = NSUUID(UUIDString: json["id"].stringValue)
                return User(id: userId!)
            }
    }
    
    ///
    /// Register the user given the username and password
    ///
    func register(email: String, password: String, f: Result<User> -> Void) -> Void {
        request(LiftServerURLs.UserRegister(), body: .Json(params: [ "email": email, "password": password ]))
            .responseAsResutlt(f) { json -> User in
                let userId = NSUUID(UUIDString: json["id"].stringValue)
                return User(id: userId!)
            }
        
    }
    
}
