import Foundation

struct User {
    var id: NSUUID
    
    ///
    /// User's public profile
    ///
    struct Profile {
        var firstName: String
        var lastName: String
        var weight: Int?
        var age: Int?
        
        static func empty() -> Profile {
            return Profile(firstName: "", lastName: "", weight: nil, age: nil)
        }
    }
}

