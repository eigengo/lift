import Foundation

struct User {
    var uuid: NSUUID
}

struct LiftServerRequest {
    var path: String
    var method: Method
    var body: Body?

    private init(path: String, method: Method, body: Body) {
        self.path = path
        self.method = method
        self.body = body
    }
        
    init(path: String, method: Method) {
        self.path = path
        self.method = method
        self.body = nil
    }
    
    func withJsonBody(params: [String : AnyObject]) -> LiftServerRequest {
        return LiftServerRequest(path: self.path, method: self.method, body: Body.Json(params: params))
    }
    
    func withDataBody(data: NSData) -> LiftServerRequest {
        return LiftServerRequest(path: self.path, method: self.method, body: Body.Data(data: data))
    }
    
    enum Body {
        case Json(params: [String : AnyObject])
        case Data(data: NSData)
    }
}

protocol LiftServerRequestConvertible {
    var Request: LiftServerRequest { get }
}

enum LiftServerURLs : LiftServerRequestConvertible {
    case UserRegister(email: String, password: String)
    case UserLogin(email: String, password: String)
    case UserRegisterDevice(userId: NSUUID)
    case UserGetProfile(userId: NSUUID)

    case ExerciseGetAllExercises(userId: NSUUID)
    
    case ExerciseSessionStart(userId: NSUUID)
    case ExerciseSessionSubmitData(userId: NSUUID, sessionId: NSUUID)
    case ExerciseSessionEnd(userId: NSUUID, sessionId: NSUUID)
    
    // MARK: URLStringConvertible
    var Request: LiftServerRequest {
        get {
            let r: LiftServerRequest = {
                switch self {
                case let .UserRegister(email, password): return LiftServerRequest(path: "/user", method: Method.POST).withJsonBody([ "email": email, "password": password ])
                case let .UserLogin(email, password): return LiftServerRequest(path: "/user", method: Method.PUT).withJsonBody([ "email": email, "password": password ])
                    
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

public class LiftServer {
    private func error(code: Int) -> NSError {
        return NSError(domain: "com.eigengo.lift", code: code, userInfo: nil)
    }

    public class var sharedInstance: LiftServer {
        struct Singleton {
            static let instance = LiftServer()
        }
        
        return Singleton.instance
    }
    
    var deviceToken: NSData?
    let baseURLString = "http://localhost:12552"
    
    
    private func liftRequest(req: LiftServerRequestConvertible) -> Request {
        let lsr = req.Request
        switch lsr.body {
        case let .Some(LiftServerRequest.Body.Json(params)): return request(lsr.method, baseURLString + lsr.path, parameters: params, encoding: ParameterEncoding.JSON)
        case let .Some(LiftServerRequest.Body.Data(data)): return upload(lsr.method, baseURLString + lsr.path, data)
        case .None: return request(lsr.method, baseURLString + lsr.path, parameters: nil, encoding: ParameterEncoding.URL)
        }
    }
    
    func login(email: String, password: String) -> Result<User> {
        liftRequest(LiftServerURLs.UserLogin(email: email, password: password))
            .responseSwiftyJSON { (_, _, json, error) in
                
            }
        
        return Result.value(User(uuid: NSUUID()))
    }
    
    func register<U>(email: String, password: String, f: Result<User> -> U) -> Void {
        liftRequest(LiftServerURLs.UserRegister(email: email, password: password))
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
