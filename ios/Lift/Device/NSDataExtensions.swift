import Foundation

func asBigEndian(value: UInt16) -> [UInt8] {
    return [
        (UInt8)(value & 0x00ff),
        (UInt8)((value & 0xff00) >> 8),
    ]
}

func asBigEndian(value: UInt32) -> [UInt8] {
    return [
        (UInt8) (value & 0x000000ff),
        (UInt8)((value & 0x0000ff00) >> 8),
        (UInt8)((value & 0x00ff0000) >> 16),
        (UInt8)((value & 0xff000000) >> 24),
    ]
}

class NSMutableDataExtensions {
    
    class func appendBytes(this: NSMutableData, value: [UInt8]) -> Void {

        var bytes = value
        this.appendBytes(&bytes, length: value.count)
    }
    
    class func appendUInt8(this: NSMutableData, value: UInt8) -> Void {
        NSMutableDataExtensions.appendBytes(this, value: [value])
    }
    
    class func appendUInt16(this: NSMutableData, value: UInt16) -> Void {
        NSMutableDataExtensions.appendBytes(this, value: asBigEndian(value))
    }
    
    class func appendUInt32(this: NSMutableData, value: UInt32) -> Void {
        NSMutableDataExtensions.appendBytes(this, value: asBigEndian(value))
    }
    
}