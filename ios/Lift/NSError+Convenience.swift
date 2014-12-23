import Foundation

extension NSError {
    
    public class func errorWithMessage(message: String, code: Int) -> NSError {
        let userInfo = [NSLocalizedDescriptionKey : message]
        let err = NSError(domain: "com.eigengo.lift", code: code, userInfo: userInfo)
        return err
    }
}