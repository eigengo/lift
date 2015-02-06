import Foundation
import XCTest

class ByteVector {
    
    let bytes: [UInt8]
    var currentPosition = 0
    
    init (_ bytes: [UInt8]) {
        self.bytes = bytes
    }
    
    func assert(test: Bool) -> Void {
        XCTAssertTrue(test)
    }
    
    func drop(count: Int) -> ByteVector {
        currentPosition += count
        return self
    }
    
    func readUInt8() -> Int {
        assert(currentPosition < bytes.count)
        return Int(bytes[currentPosition++])
    }
    
    func readUInt16() -> Int {
        assert(currentPosition + 1 < bytes.count)
        return (Int(bytes[currentPosition++]) << 8) + (Int(bytes[currentPosition++]))
    }
    
    func readByteArray(count: Int) -> [UInt8] {
        assert(currentPosition + count - 1 < bytes.count)
        let result = Array(bytes[currentPosition..<currentPosition + count])
        currentPosition += count
        return result
    }
    
}