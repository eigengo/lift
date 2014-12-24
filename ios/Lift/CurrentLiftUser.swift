import Foundation

///
/// Holds the currently logged-in user
///
struct CurrentLiftUser {
    private static var _userId: NSUUID?
    
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