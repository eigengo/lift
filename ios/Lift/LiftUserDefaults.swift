import Foundation

/**
 * Settings and defaults for the lift app
 */
struct LiftUserDefaults {
    static var deviceLocations: [DeviceId : DeviceInfo.Location] = [:]
    
    private static func getLocation(deviceId id: DeviceId) -> DeviceInfo.Location {
        if let x = deviceLocations[id] { return x }
        let value = UInt8(NSUserDefaults.standardUserDefaults().integerForKey("\(id.UUIDString)-location"))
        return DeviceInfo.Location(rawValue: value) ?? DeviceInfo.Location.Any
    }
    
    private static func setLocation(deviceId id: DeviceId, location: DeviceInfo.Location) {
        let value = Int(location.rawValue)
        NSUserDefaults.standardUserDefaults().setInteger(value, forKey: "\(id.UUIDString)-location")
    }
    
    ///
    /// Gets the location given the ``deviceInfo``
    ///
    static func getLocation(#deviceInfo: DeviceInfo) -> DeviceInfo.Location {
        switch deviceInfo {
        case .ConnectedDeviceInfo(id: let id, type: _, name: _, description: _): return getLocation(deviceId: id)
        case .DisconnectedDeviceInfo(id: let id, type: _, error: _): return getLocation(deviceId: id)
        default: return DeviceInfo.Location.Any
        }
    }
    
    ///
    /// Sets the new ``location`` of the given ``deviceInfo``
    ///
    static func setLocation(#deviceInfo: DeviceInfo, location: DeviceInfo.Location) {
        switch deviceInfo {
        case .ConnectedDeviceInfo(id: let id, type: _, name: _, description: _): setLocation(deviceId: id, location: location)
        case .DisconnectedDeviceInfo(id: let id, type: _, error: _): setLocation(deviceId: id, location: location)
        default: return
        }
    }
    
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
    
    ///
    /// Returns ``true`` if the application is being launched to run tests
    ///
    static var isRunningTests: Bool {
        get {
            let environment = NSProcessInfo.processInfo().environment
            if let injectBundle = environment["XCInjectBundle"] as? String {
                return injectBundle.pathExtension == "xctest"
            }
            return false
        }
    }
    
    ///
    /// Returns the configured Lift server URL, or a default value. The value is set in the
    /// settings bundle for the application. See ``Settings.bundle``.
    ///
    static var liftServerUrl: String {
        get {
            if let url = NSUserDefaults.standardUserDefaults().stringForKey("liftServerUrl") {
                return url
            } else {
                return "http://localhost:12551"
            }
        }
    }
    
    ///
    /// Returns ``true`` if the user has selected to show session details. The value is set in the
    /// settings bundle for the application. See ``Settings.bundle``.
    ///
    static var showSessionDetails: Bool {
        get {
            return NSUserDefaults.standardUserDefaults().boolForKey("showSessionDetails")
        }
    }
    
}