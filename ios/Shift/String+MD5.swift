import Foundation

extension String {
    
    func md5UUID() -> NSUUID {
        let data = self.dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: false)!
        let md = UnsafeMutablePointer<UInt8>.alloc(Int(CC_MD5_DIGEST_LENGTH))
        CC_MD5(data.bytes, CC_LONG(data.length), md)
        return NSUUID(UUIDBytes: md)
    }

}