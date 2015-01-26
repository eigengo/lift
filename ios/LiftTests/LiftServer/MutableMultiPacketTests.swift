import XCTest

class MutableMultiPacketTests : XCTestCase {
   
    func testBuild() {
        let mp = MutableMultiPacket()
        mp.appendPayload(SensorDataSourceLocation.Wrist, payload: 0xff, 0x01, 0x02, 0x03)
        mp.appendPayload(SensorDataSourceLocation.Waist, payload: 0xf0, 0x01)
        mp.appendRepeatedPayload(SensorDataSourceLocation.Any, size: 0xffff, const: 0x8f)
        let result = mp.data()
        
        // the output should be
        // 0x00:               0xca 0xb0 0x03 0xt0 0xt1 0xt2 0xt3 (header)
        // h + 0x00:           0x00 0x04 0x01 (4 B at wrist)  0xff 0x01 0x02 0x03 (payload 0)
        // h + _1 + 0x00:      0x00 0x02 0x02 (2 B at waist)  0xf0 0x01 (payload 1)
        // h + _1 + _2 + 0x10: 0xff 0xff 0x7f (64 kiB at any) 0x7f 0x7f ... (payload 2)
        
        let h = 7
        let _1 = 3 + 4
        let _2 = 3 + 2

        let x = UnsafePointer<UInt8>(result.bytes)
        XCTAssert(x[0x00] == 0xca, "Bad header 0")
        XCTAssert(x[0x01] == 0xb1, "Bad header 1")
        XCTAssert(x[0x02] == 0x03, "Bad header count")
        
        XCTAssert(x[h + 0x00] == 0x00, "Bad D header 0 size h")
        XCTAssert(x[h + 0x01] == 0x04, "Bad D header 0 size l")
        XCTAssert(x[h + 0x02] == 0x01, "Bad D header 0 sdsl")
        XCTAssert(x[h + 0x03] == 0xff, "Bad data 0 0") // 8, 9, 10
        // ...
        
        XCTAssert(x[h + _1 + 0x00] == 0x00, "Bad D header 1 size h")
        XCTAssert(x[h + _1 + 0x01] == 0x02, "Bad D header 1 size l")
        // ...
        
        XCTAssert(x[h + _1 + _2 + 0x00] == 0xff, "Bad D header 2 size h")
        XCTAssert(x[h + _1 + _2 + 0x01] == 0xff, "Bad D header 2 size l")
        XCTAssert(x[h + _1 + _2 + 0x02] == 0x7f, "Bad D header 2 sdsl")
        XCTAssert(x[h + _1 + _2 + 0x03] == 0x8f, "Bad data 2 0")
        XCTAssert(x[h + _1 + _2 + 0xffff] == 0x8f, "Bad data 2 0xffff")
    }
    
}