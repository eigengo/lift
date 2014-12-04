import Foundation

struct User {
    var id: NSUUID
    
    ///
    /// User's public profile
    ///
    struct PublicProfile {
        var firstName: String
        var lastName: String
        var weight: Int?
        var age: Int?
    }
}

