import XCTest

class MutableMultiPacketTests : XCTestCase {
    private func payload(bytes: Byte...) -> NSData {
        return NSData(bytes: bytes, length: bytes.count)
    }
    
    private func repeatedPayload(size: UInt16, const: UInt8) -> NSData {
        let buffer: [UInt8] = [UInt8](count: Int(size), repeatedValue: const)
        return NSData(bytes: buffer, length: Int(size))
    }
    
    func testBuild() {
        let mp = MutableMultiPacket()
        mp.append(SensorDataSourceLocation.Wrist, data: payload(0xff, 0x01, 0x02, 0x03))
        mp.append(SensorDataSourceLocation.Waist, data: payload(0xf0, 0x01))
        mp.append(SensorDataSourceLocation.Any,   data: repeatedPayload(0xffff, const: 0x8f))
        let result = mp.data()
        
        // the output should be
        // 0x00: 0xca 0xb0 0x03 (header)
        // 0x03: 0x00 0x04 0x01 (4 B at wrist)  0xff 0x01 0x02 0x03 (payload 0)
        // 0x0a: 0x00 0x02 0x02 (2 B at waist)  0xf0 0x01 (payload 1)
        // 0x0f: 0xff 0xff 0x7f (64 kiB at any) 0x7f 0x7f ... (payload 2)
        
        let x = UnsafePointer<UInt8>(result.bytes)
        XCTAssert(x[0x00] == 0xca, "Bad header 0")
        XCTAssert(x[0x01] == 0xb0, "Bad header 1")
        XCTAssert(x[0x02] == 0x03, "Bad header count")
        
        XCTAssert(x[0x03] == 0x00, "Bad D header 0 size h")
        XCTAssert(x[0x04] == 0x04, "Bad D header 0 size l")
        XCTAssert(x[0x05] == 0x01, "Bad D header 0 sdsl")
        XCTAssert(x[0x06] == 0xff, "Bad data 0 0") // 8, 9, 10
        // ...
        
        XCTAssert(x[0x0a] == 0x00, "Bad D header 1 size h")
        XCTAssert(x[0x0b] == 0x02, "Bad D header 1 size l")
        // ...
        
        XCTAssert(x[0x0f] == 0xff, "Bad D header 2 size h")
        XCTAssert(x[0x10] == 0xff, "Bad D header 2 size l")
        XCTAssert(x[0x11] == 0x7f, "Bad D header 2 sdsl")
        XCTAssert(x[0x12] == 0x8f, "Bad data 2 0")
        XCTAssert(x[0xffff + 0x11] == 0x8f, "Bad data 2 0xffff")
    }
    
}