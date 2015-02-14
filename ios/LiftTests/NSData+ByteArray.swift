import Foundation

extension NSData {
    
    ///
    /// Views this NSData as array of bytes
    ///
    func asBytes() -> [UInt8] {
        var buf = [UInt8](count: length, repeatedValue: 0)
        getBytes(&buf, length: length)
        return buf
    }

}