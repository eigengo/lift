import Foundation

struct User {
    var uuid: NSUUID
}

public class LiftServer {
    private func error(code: Int) -> NSError {
        return NSError(domain: "com.eigengo.lift", code: code, userInfo: nil)
    }

    public class var sharedInstance: LiftServer {
        struct Singleton {
            static let instance = LiftServer()
        }
        
        return Singleton.instance
    }
    
    var deviceToken: NSData?
    
    func login(username: String, password: String) -> Result<User> {
        return Result.value(User(uuid: NSUUID()))
    }
    
    func register(username: String, password: String) -> Result<User> {
        return Result.error(error(-1))
    }
    
}
