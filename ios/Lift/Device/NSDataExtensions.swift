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

extension NSMutableData {
    
    func appendBytes(value: [UInt8]) -> Void {

        var bytes = value
        self.appendBytes(&bytes, length: value.count)
    }
    
    func appendUInt8(value: UInt8) -> Void {
        self.appendBytes([value])
    }
    
    func appendUInt16(value: UInt16) -> Void {
        self.appendBytes(asBigEndian(value))
    }
    
    func appendUInt32(value: UInt32) -> Void {
        self.appendBytes(asBigEndian(value))
    }
    
    func extractBytes() -> [UInt8] {
        var array = [UInt8](count: self.length, repeatedValue: 0x00)
        self.getBytes(&array, length: self.length)
        return array
    }
    
}