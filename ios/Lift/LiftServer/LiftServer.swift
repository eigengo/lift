import Foundation

///
/// Adds the response negotiation
///
extension Request {
    
    public func responseAsResutlt<A, U>(f: Result<A> -> U, completionHandler: (JSON) -> A) -> Void {
        responseSwiftyJSON { (request, response, json, error) -> Void in
            if let x = response {
                if x.statusCode != 200 {
                    let userInfo = [NSLocalizedDescriptionKey : json.stringValue]
                    let err = NSError(domain: "com.eigengo.lift", code: x.statusCode, userInfo: userInfo)
                    NSLog("Failed with %@ -> %@", request, x)
                    f(Result.error(err))
                } else {
                    let val = completionHandler(json)
                    f(Result.value(val))
                }
            } else if let x = error {
                NSLog("Failed with %@", x)
                f(Result.error(x))
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
        
        configuration.requestCachePolicy = NSURLRequestCachePolicy.UseProtocolCachePolicy
        configuration.timeoutIntervalForRequest = NSTimeInterval(5) // default timeout
        
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
    
    private func baseURLString() -> String {
        return LiftUserDefaults.liftServerUrl
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
    private func request(req: LiftServerRequestConvertible, body: Body? = nil) -> Request {
        let lsr = req.Request
        switch body {
        case let .Some(Body.Json(params)): return manager.request(lsr.method, baseURLString() + lsr.path, parameters: params, encoding: ParameterEncoding.JSON)
        case let .Some(Body.Data(data)): return manager.upload(URLRequest(lsr.method, baseURLString() + lsr.path), data: data)
        case .None: return manager.request(lsr.method, baseURLString() + lsr.path, parameters: nil, encoding: ParameterEncoding.URL)
        }
    }
    
    // MARK: - User profile
    
    ///
    /// Register the iOS push device token for the given user
    ///
    func userRegisterDeviceToken(userId: NSUUID, deviceToken: NSData) -> Void {
        let bytes = UnsafePointer<UInt8>(deviceToken.bytes)
        var tokenBytes: [NSNumber] = []
        for var i = 0; i < deviceToken.length; ++i {
            tokenBytes.append(NSNumber(unsignedChar: bytes.advancedBy(i).memory))
        }
        request(LiftServerURLs.UserRegisterDevice(userId), body: .Json(params: [ "deviceToken": tokenBytes ]))
            .responseString { (_, _, body, error) -> Void in
                // println(body)
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
            .responseAsResutlt(f, User.PublicProfile.unmarshal)
    }
    
    ///
    /// Sets the public profile for the given ``userId``
    ///
    func userSetPublicProfile(userId: NSUUID, profile: User.PublicProfile, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.UserSetPublicProfile(userId), body: .Json(params: profile.marshal()))
            .responseAsResutlt(f, const())
    }
    
    ///
    /// Checks that the account is still valid
    ///
    func userCheckAccount(userId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.UserCheckAccount(userId))
            .responseAsResutlt(f, const(()))
    }
    
    func userGetProfileImage(userId: NSUUID, f: Result<NSData> -> Void) -> Void {
        request(LiftServerURLs.UserGetProfileImage(userId))
            .response { (_, response: NSHTTPURLResponse?, responseBody, err) in
                let body = responseBody as? NSData
                if let x = response {
                    if x.statusCode != 200 {
                        f(Result.error(NSError.errorWithMessage("Request failed", code: x.statusCode)))
                    } else {
                        if let b = body { f(Result.value(b)) } else { f(Result.error(NSError.errorWithMessage("No body", code: x.statusCode)))}
                    }
                } else if let e = err {
                    f(Result.error(e))
                }
            }
    }
    
    func userSetProfileImage(userId: NSUUID, image: NSData, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.UserSetProfileImage(userId), body: .Data(data: image))
            .responseAsResutlt(f, const(()))
    }
    
    // MARK: - Classifiers
    
    ///
    /// Get known / classifiable muscle groups
    ///
    func exerciseGetMuscleGroups(f: Result<[Exercise.MuscleGroup]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetMuscleGroups())
            .responseAsResutlt(f) { json -> [Exercise.MuscleGroup] in
                return json.arrayValue.map(Exercise.MuscleGroup.unmarshal)
            }
    }
    
    // Mark: - Exercise session

    ///
    /// Start exercise session for the user, with the props
    ///
    func exerciseSessionStart(userId: NSUUID, props: Exercise.SessionProps, f: Result<NSUUID> -> Void) -> Void {
        let startDateString = isoDateFormatter.stringFromDate(props.startDate)
        let params: [String : AnyObject] = [
            "startDate": startDateString,
            "muscleGroupKeys": props.muscleGroupKeys,
            "intendedIntensity": props.intendedIntensity
        ]
        request(LiftServerURLs.ExerciseSessionStart(userId), body: .Json(params: params))
            .responseAsResutlt(f) { json in
                return NSUUID(UUIDString: json["id"].stringValue)!
            }
    }

    ///
    /// Submit data (received from the smartwatch most likely) to the running session
    ///
    func exerciseSessionSubmitData(userId: NSUUID, sessionId: NSUUID, data: NSData, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionSubmitData(userId, sessionId), body: .Data(data: data))
            .responseAsResutlt(f, const(()))
    }
    
    ///
    /// Close the running session
    ///
    func exerciseSessionEnd(userId: NSUUID, sessionId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionEnd(userId, sessionId))
            .responseAsResutlt(f, const(()))
    }
    
    ///
    /// Get summary of all sessions
    ///
    func exerciseGetExerciseSessionsSummary(userId: NSUUID, date: NSDate, f: Result<[Exercise.SessionSummary]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetExerciseSessionsSummary(userId, date))
            .responseAsResutlt(f) { json -> [Exercise.SessionSummary] in
                return json.arrayValue.map(Exercise.SessionSummary.unmarshal)
            }
    }
    
    func exerciseGetExerciseSessionsDates(userId: NSUUID, f: Result<[Exercise.SessionDate]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetExerciseSessionsDates(userId))
            .responseAsResutlt(f) { json in return json.arrayValue.map(Exercise.SessionDate.unmarshal) }
    }

    ///
    /// Get one particular session
    ///
    func exerciseGetExerciseSession(userId: NSUUID, sessionId: NSUUID, f: Result<Exercise.ExerciseSession> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetExerciseSession(userId, sessionId))
            .responseAsResutlt(f, Exercise.ExerciseSession.unmarshal)
    }
    
    func exerciseDeleteExerciseSession(userId: NSUUID, sessionId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseDeleteExerciseSession(userId, sessionId))
            .responseAsResutlt(f, const(()))
    }
}
