import Foundation

extension User.PublicProfile {
 
    static func unmarshal(json: JSON) -> User.PublicProfile? {
        if json.isEmpty {
            return nil
        } else {
            let x = json["image"].arrayValue.map { $0.uInt8Value }
            return User.PublicProfile(firstName: json["firstName"].stringValue,
                lastName: json["lastName"].stringValue,
                weight: json["weight"].int,
                age: json["age"].int,
                image: x)
        }
    }
    
    func marshal() -> [String : AnyObject] {
        var params: [String : AnyObject] = ["firstName": firstName, "lastName": lastName]
        params["age"] = age?
        params["weight"] = weight?
        if let x = image {
            let hack: [Int] = x.map { x in return Int(x) }
            params["image"] = hack
        }
        
        return params
    }

}