import Foundation
import XCTest

class SensorDataProtocolTests : XCTestCase {
    
    func testEncodingMatchesPebble() {
        var buf: [UInt8] = [0, 0, 0, 0, 0]
        //ff ff ff ff 01
        encode_lift_accelerometer_data(-1, -1, 127, &buf)
        XCTAssertArraysEqual(buf, [0xff, 0xff, 0xff, 0xff, 0x01])
        
        //00 00 00 00 01
        encode_lift_accelerometer_data(0, 0, 64, &buf)
        XCTAssertArraysEqual(buf, [0x00, 0x00, 0x00, 0x00, 0x01])
        
        //78 01 4a c0 73
        encode_lift_accelerometer_data(376, 592, -784, &buf)
        XCTAssertArraysEqual(buf, [0x78, 0x01, 0x4a, 0xc0, 0x73])
    }
    
}
