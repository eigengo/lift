import Foundation

/**
 * Settings and defaults for the lift app
 */
struct LiftUserDefaults {
   
    /**
     * Writes the current value of the logged-in user
     */
    static func setCurrentUserId(userId: NSUUID?) {
        if !isRunningTests {
            if userId == nil {
                NSUserDefaults.standardUserDefaults().setNilValueForKey("userId")
            } else {
                NSUserDefaults.standardUserDefaults().setValue(userId!.UUIDString, forKey: "userId")
            }
        }
    }
    
    /**
     * Retrieves the last known logged-in user
     */
    static func getCurrentUserId() -> NSUUID? {
        if isRunningTests {
            return NSUUID(UUIDString: "00000000-0000-0000-0000-000000000000")!
        } else {
            if let x = NSUserDefaults.standardUserDefaults().stringForKey("userId") {
                return NSUUID(UUIDString: x)
            }
            return nil
        }
    }
    
    static var isRunningTests: Bool {
        get {
            let environment = NSProcessInfo.processInfo().environment
            if let injectBundle = environment["XCInjectBundle"] as? String {
                return injectBundle.pathExtension == "xctest"
            }
            return false
        }
    }
    
    static var liftServerUrl: String {
        get {
            if let url = NSUserDefaults.standardUserDefaults().stringForKey("lift_server_url") {
                return url
            } else {
                return "http://localhost:12551"
            }
        }
    }
    
    static var showSessionDetails: Bool {
        get {
            return NSUserDefaults.standardUserDefaults().boolForKey("showSessionDetails")
        }
    }
    
}