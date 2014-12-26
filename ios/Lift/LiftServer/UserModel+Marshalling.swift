import Foundation

extension User.PublicProfile {
 
    static func unmarshal(json: JSON) -> User.PublicProfile? {
        if json.isEmpty {
            return nil
        } else {
            return User.PublicProfile(firstName: json["firstName"].stringValue,
                lastName: json["lastName"].stringValue,
                weight: json["weight"].int,
                age: json["age"].int)
        }
    }
    
    func marshal() -> [String : AnyObject] {
        var params: [String : AnyObject] = ["firstName": firstName, "lastName": lastName]
        params["age"] = age?
        params["weight"] = weight?
        
        return params
    }

}