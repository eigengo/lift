import Foundation

extension NSError {
    
    public class func notImplemented() -> NSError {
        return errorWithMessage("NSError.notImplemented".localized(), code: 666)
    }
    
    public class func errorWithMessage(message: String, code: Int) -> NSError {
        let userInfo = [NSLocalizedDescriptionKey : message]
        let err = NSError(domain: "com.eigengo.lift", code: code, userInfo: userInfo)
        return err
    }
}