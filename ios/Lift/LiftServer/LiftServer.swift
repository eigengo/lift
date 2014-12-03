import Foundation

struct User {
    var id: NSUUID

    struct PublicProfile {
        var firstName: String
        var lastName: String
        var weight: Int?
        var age: Int?
    }
}

struct Exercise {

    ///
    /// Single muscle group cell holding the key, which is the communication key with the server
    /// together with the (localisable) title and set of exercises
    ///
    struct MuscleGroup {
        /// the key that identifies the muscle group to the server
        var key: String
        /// the title—in the future received from the server (localised)
        var title: String
        /// the sample/suggested exercises—in the future received from the server (localised)
        var exercises: [String]
    }
    
    ///
    /// Session properties
    ///
    struct SessionProps {
        /// the start date
        var startDate: NSDate
        /// the targeted muscle groups
        var muscleGroupKeys: [String]
        /// the intended intensity
        var intendedIntensity: Double
    }
    
    ///
    /// Sigle exercise
    ///
    struct Exercise {
        var name: String
        var intensity: Double?
    }
    
    ///
    /// Association of list of exercise with a particular session props
    ///
    struct Exercises {
        var props: SessionProps
        var exercises: [Exercise]
    }
    
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
    case UserGetPublicProfile(/*userId: */NSUUID)
    
    ///
    /// Sets the user's profile for the ``userId``
    ///
    case UserSetPublicProfile(/*userId: */NSUUID)

    ///
    /// Get supported muscle groups
    ///
    case ExerciseGetMuscleGroups()
    
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
                case .UserGetPublicProfile(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)", method: Method.GET)
                case .UserSetPublicProfile(let userId): return LiftServerRequest(path: "/user/\(userId.UUIDString)", method: Method.POST)
                
                case .ExerciseGetMuscleGroups(): return LiftServerRequest(path: "/exercise/musclegroups", method: Method.GET)
                    
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
    private let isoDateFormatter: NSDateFormatter = {
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        return dateFormatter
    }()
    
    //private let baseURLString = "http://192.168.59.103:49154"
    private let baseURLString = "http://192.168.0.6:12551"
    //private let baseURLString = "http://192.168.101.102:12551"
    
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
        case let .Some(Body.Data(data)): return manager.upload(URLRequest(lsr.method, baseURLString + lsr.path), data: data)
        case .None: return manager.request(lsr.method, baseURLString + lsr.path, parameters: nil, encoding: ParameterEncoding.URL)
        }
    }
    
    // MARK: - User profile
    
    ///
    /// Register the iOS push device token for the given user
    ///
    func userRegisterDeviceToken(userId: NSUUID, deviceToken: NSData) -> Void {
        let tokenString = NSString(data: deviceToken, encoding: NSASCIIStringEncoding)!
        request(LiftServerURLs.UserRegisterDevice(userId), body: .Json(params: [ "deviceToken": tokenString ]))
            .responseString { (_, _, body, error) -> Void in
                println(body)
            }
        
    }
    
    ///
    /// Login the user given the username and password
    ///
    func userLogin(email: String, password: String, f: Result<User> -> Void) -> Void {
        request(LiftServerURLs.UserLogin(), body: .Json(params: [ "email": email, "password": password ]))
            .responseAsResutlt(f) { json -> User in
                let userId = NSUUID(UUIDString: json["id"].stringValue)
                return User(id: userId!)
            }
    }
    
    ///
    /// Register the user given the username and password
    ///
    func userRegister(email: String, password: String, f: Result<User> -> Void) -> Void {
        request(LiftServerURLs.UserRegister(), body: .Json(params: [ "email": email, "password": password ]))
            .responseAsResutlt(f) { json -> User in
                let userId = NSUUID(UUIDString: json["id"].stringValue)
                return User(id: userId!)
            }
        
    }

    ///
    /// Get the public profile for the given ``userId``
    ///
    func userGetPublicProfile(userId: NSUUID, f: Result<User.PublicProfile?> -> Void) -> Void {
        request(LiftServerURLs.UserGetPublicProfile(userId))
            .responseAsResutlt(f) { json -> User.PublicProfile? in
                if json.isEmpty {
                    return nil
                } else {
                    return User.PublicProfile(firstName: json["firstName"].stringValue,
                        lastName: json["lastName"].stringValue,
                        weight: json["weight"].int,
                        age: json["age"].int)
                }
            }
    }
    
    ///
    /// Sets the public profile for the given ``userId``
    ///
    func userSetPublicProfile(userId: NSUUID, publicProfile: User.PublicProfile, f: Result<Void> -> Void) -> Void {
        var params: [String : AnyObject] = ["firstName": publicProfile.firstName, "lastName": publicProfile.lastName]
        if publicProfile.age != nil {
            params["age"] = publicProfile.age!
        }
        if publicProfile.weight != nil {
            params["weight"] = publicProfile.weight!
        }
        
        request(LiftServerURLs.UserSetPublicProfile(userId), body: .Json(params: params))
            .responseAsResutlt(f) { json in }
    }
    
    // MARK: - Exercise
    
    func exerciseGetMuscleGroups(f: Result<[Exercise.MuscleGroup]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetMuscleGroups())
            .responseAsResutlt(f) { json -> [Exercise.MuscleGroup] in
                return json.arrayValue.map { mg -> Exercise.MuscleGroup in
                    return Exercise.MuscleGroup(
                        key: mg["key"].stringValue,
                        title: mg["title"].stringValue,
                        exercises: mg["exercises"].arrayValue.map { $0.stringValue }
                    )
                }
            }
    }
    
    // Mark: - Exercise session
    
    func exerciseSessionStart(userId: NSUUID, props: Exercise.SessionProps, f: Result<NSUUID> -> Void) -> Void {
        let startDateString = isoDateFormatter.stringFromDate(props.startDate)
        let params: [String : AnyObject] = [
            "startDate": startDateString,
            "muscleGroupKeys": props.muscleGroupKeys,
            "intendedIntensity": props.intendedIntensity
        ]
        request(LiftServerURLs.ExerciseSessionStart(userId), body: .Json(params: params))
            .responseAsResutlt(f) { json in
                println(json)
                return NSUUID(UUIDString: json["id"].stringValue)!
            }
    }
    
    func exerciseSessionSubmitData(userId: NSUUID, sessionId: NSUUID, data: NSData, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionSubmitData(userId, sessionId), body: .Data(data: data))
            .responseAsResutlt(f) { json in }
    }
    
    func exerciseSessionEnd(userId: NSUUID, sessionId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionEnd(userId, sessionId))
            .responseAsResutlt(f) { json in }
    }
    
    func exerciseGetAllExercises(userId: NSUUID, f: Result<[Exercise.SessionProps]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetAllExercises(userId))
            .responseAsResutlt(f) { json -> [Exercise.SessionProps] in
                return json.arrayValue.map { json -> Exercise.SessionProps in
                    let startDate = self.isoDateFormatter.dateFromString(json["startDate"].stringValue)!
                    let muscleGroupKeys = json["muscleGroupKeys"].arrayValue.map { $0.stringValue }
                    let intendedIntensity = json["intendedIntensity"].doubleValue
                    return Exercise.SessionProps(startDate: startDate, muscleGroupKeys: muscleGroupKeys, intendedIntensity: intendedIntensity)
                }
            }
    }
}
