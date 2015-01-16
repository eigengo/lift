import Foundation

struct AvailabilityState {
    var isReachable: Bool = true
    var lastServerFailureDate: NSDate? = nil
    
    func shouldAttemptRequest() -> Bool {
        if isReachable {
            if let x = lastServerFailureDate {
                return NSDate().timeIntervalSinceDate(x) > 5
            }
            return true
        }
        return false
    }
    
    func failed() -> AvailabilityState {
        return AvailabilityState(isReachable: isReachable, lastServerFailureDate: NSDate())
    }
    
    func reachable() -> AvailabilityState {
        return AvailabilityState(isReachable: true, lastServerFailureDate: lastServerFailureDate)
    }
    
    func unreachable() -> AvailabilityState {
        return AvailabilityState(isReachable: false, lastServerFailureDate: lastServerFailureDate)
    }
}


///
/// Adds the response negotiation
///
extension Request {
    typealias AvailabilityStateUpdate = (AvailabilityState, AvailabilityState -> Void)
    
    func responseAsResutlt<A, U>(asu: AvailabilityStateUpdate, f: Result<A> -> U, completionHandler: (JSON) -> A) -> Void {
        let (s, u) = asu
        
        if s.shouldAttemptRequest() {
            responseSwiftyJSON { (request, response, json, error) -> Void in
                if let x = response {
                    // we have a valid response
                    let statusCodeFamily = x.statusCode / 100
                    if statusCodeFamily == 1 || statusCodeFamily == 2 || statusCodeFamily == 3 {
                        // 1xx, 2xx, 3xx responses are success responses
                        let val = completionHandler(json)
                        f(Result.value(val))
                    } else {
                        // 4xx responses are errors, but do not mean that the server is broken
                        let userInfo = [NSLocalizedDescriptionKey : json.stringValue]
                        let err = NSError(domain: "com.eigengo.lift", code: x.statusCode, userInfo: userInfo)
                        NSLog("Failed with %@ -> %@", request, x)
                        f(Result.error(err))
                    }
                    if statusCodeFamily == 5 {
                        // we have 5xx responses. this counts as server error.
                        u(s.failed())
                    }
                } else if let x = error {
                    // we don't have a responses, and we have an error
                    NSLog("Failed with %@", x)
                    u(s.failed())
                    f(Result.error(x))
                }
            }
        } else {
            f(Result.error(NSError.errorWithMessage("Server unavailable", code: 999)))
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
    
    private func registerReachability() {
        let reachability = Reachability.reachabilityForInternetConnection()
        reachability.reachableBlock = { _ in
            self.availabilityState = self.availabilityState.reachable()
        }
        reachability.unreachableBlock = { _ in
            self.availabilityState = self.availabilityState.unreachable()
        }
    }

    /// indicates that the server is reachable
    private var availabilityState = AvailabilityState()
    
    private func asu() -> (AvailabilityState, AvailabilityState -> Void) {
        return (availabilityState, { x in self.availabilityState = x })
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
            .responseAsResutlt(asu(), const(()), const(()))
        
    }
    
    ///
    /// Login the user given the username and password
    ///
    func userLogin(email: String, password: String, f: Result<User> -> Void) -> Void {
        request(LiftServerURLs.UserLogin(), body: .Json(params: [ "email": email, "password": password ]))
            .responseAsResutlt(asu(), f) { json -> User in
                let userId = NSUUID(UUIDString: json["id"].stringValue)
                return User(id: userId!)
            }
    }
    
    ///
    /// Register the user given the username and password
    ///
    func userRegister(email: String, password: String, f: Result<User> -> Void) -> Void {
        request(LiftServerURLs.UserRegister(), body: .Json(params: [ "email": email, "password": password ]))
            .responseAsResutlt(asu(), f) { json -> User in
                let userId = NSUUID(UUIDString: json["id"].stringValue)
                return User(id: userId!)
            }
        
    }

    ///
    /// Get the public profile for the given ``userId``
    ///
    func userGetPublicProfile(userId: NSUUID, f: Result<User.PublicProfile?> -> Void) -> Void {
        request(LiftServerURLs.UserGetPublicProfile(userId))
            .responseAsResutlt(asu(), f, User.PublicProfile.unmarshal)
    }
    
    ///
    /// Sets the public profile for the given ``userId``
    ///
    func userSetPublicProfile(userId: NSUUID, profile: User.PublicProfile, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.UserSetPublicProfile(userId), body: .Json(params: profile.marshal()))
            .responseAsResutlt(asu(), f, const())
    }
    
    ///
    /// Checks that the account is still valid
    ///
    func userCheckAccount(userId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.UserCheckAccount(userId))
            .responseAsResutlt(asu(), f, const(()))
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
            .responseAsResutlt(asu(), f, const(()))
    }
    
    // MARK: - Classifiers
    
    ///
    /// Get known / classifiable muscle groups
    ///
    func exerciseGetMuscleGroups(f: Result<[Exercise.MuscleGroup]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetMuscleGroups())
            .responseAsResutlt(asu(), f) { json -> [Exercise.MuscleGroup] in
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
            .responseAsResutlt(asu(), f) { json in
                return NSUUID(UUIDString: json["id"].stringValue)!
            }
    }

    ///
    /// Submit data (received from the smartwatch most likely) to the running session
    ///
    func exerciseSessionSubmitData(userId: NSUUID, sessionId: NSUUID, data: MultiPacket, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionSubmitData(userId, sessionId), body: .Data(data: data.data()))
            .responseAsResutlt(asu(), f, const(()))
    }
    
    func exerciseSessionGetClassificationExamples(userId: NSUUID, sessionId: NSUUID, f: Result<[Exercise.Exercise]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionGetClassificationExamples(userId, sessionId))
            .responseAsResutlt(asu(), f) { json in json.arrayValue.map(Exercise.Exercise.unmarshal) }
    }

    ///
    /// Submit data for an explicit exerise to the server
    ///
    func exerciseSessionStartExplicitClassification(userId: NSUUID, sessionId: NSUUID, exercise: Exercise.Exercise, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExplicitExerciseClassificationStart(userId, sessionId), body: .Json(params: exercise.marshal()))
            .responseAsResutlt(asu(), f, const(()))
    }
    
    ///
    /// Finish saving data for the explicit exercise
    ///
    func exerciseSessionEndExplicitClassification(userId: NSUUID, sessionId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExplicitExerciseClassificationStop(userId, sessionId))
            .responseAsResutlt(asu(), f, const(()))
    }
    
    ///
    /// Close the running session
    ///
    func exerciseSessionEnd(userId: NSUUID, sessionId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseSessionEnd(userId, sessionId))
            .responseAsResutlt(asu(), f, const(()))
    }
    
    ///
    /// Get summary of all sessions
    ///
    func exerciseGetExerciseSessionsSummary(userId: NSUUID, date: NSDate, f: Result<[Exercise.SessionSummary]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetExerciseSessionsSummary(userId, date))
            .responseAsResutlt(asu(), f) { json -> [Exercise.SessionSummary] in
                return json.arrayValue.map(Exercise.SessionSummary.unmarshal)
            }
    }
    
    ///
    /// Get summary of session dates
    ///
    func exerciseGetExerciseSessionsDates(userId: NSUUID, f: Result<[Exercise.SessionDate]> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetExerciseSessionsDates(userId))
            .responseAsResutlt(asu(), f) { json in return json.arrayValue.map(Exercise.SessionDate.unmarshal) }
    }
    
    ///
    /// Get one particular session
    ///
    func exerciseGetExerciseSession(userId: NSUUID, sessionId: NSUUID, f: Result<Exercise.ExerciseSession> -> Void) -> Void {
        request(LiftServerURLs.ExerciseGetExerciseSession(userId, sessionId))
            .responseAsResutlt(asu(), f, Exercise.ExerciseSession.unmarshal)
    }
    
    func exerciseDeleteExerciseSession(userId: NSUUID, sessionId: NSUUID, f: Result<Void> -> Void) -> Void {
        request(LiftServerURLs.ExerciseDeleteExerciseSession(userId, sessionId))
            .responseAsResutlt(asu(), f, const(()))
    }
}
