import Foundation

///
/// Holds the currently logged-in user
///
struct CurrentLiftUser {
    private static var _userId: NSUUID?
    
    ///
    /// Contains the currently logged-in user or ``nil`` if the user 
    /// has not yet registered, or has logged out before logging back
    /// in.
    ///
    static var userId: NSUUID? {
        get {
            if _userId == nil {
                _userId = LiftUserDefaults.getCurrentUserId()
            }
            return _userId
        }
        set {
            _userId = newValue
            LiftUserDefaults.setCurrentUserId(newValue)
        }
    }
}