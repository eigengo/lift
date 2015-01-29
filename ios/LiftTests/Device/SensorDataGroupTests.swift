import Foundation
import XCTest

class SensorDataGroupTests : XCTestCase {
    let deviceId = DeviceId()

    let simpleTestData: NSData = {
        let buffer: [UInt8] = [
            0x00, 0x02, 0x02, 0x01, 0x00,  // type: 0, count: 2, samplesPerSecond: 2, sampleSize: 1, _: 0
            0x41, 0x42,                    // 'A', 'B'
            0x01, 0x02, 0x02, 0x01, 0x00,  // type: 1, count: 2, samplesPerSecond: 2, sampleSize: 1, _: 0
            0x30, 0x31,                    // '1', '2'
            0x02, 0x02, 0x02, 0x02, 0x00,  // type: 2, count: 2, samplesPerSecond: 2, sampleSize: 2, _: 0
            0x61, 0x62, 0x61, 0x63         // 'a'~'b', 'a'~'c'
        ]
        // data represents 3 sensor data arrays, each 1s long
        return NSData(bytes: buffer, length: buffer.count)
    }()

    func testTrivial() {
        var sdg = SensorDataGroup()
        
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 1)
        
        XCTAssertEqual(sdg.rawCount, 3)
        let csdas = sdg.continuousSensorDataArrays(maximumGap: 1.4, gapValue: 0, maximumDuration: 100)
    }
    
    func testTwoContinuous() {
        var sdg = SensorDataGroup()
        
        // 3 things, all starting at 1, 2 samples, 1 sps.
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 1)
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 2)
        XCTAssertEqual(sdg.rawCount, 6)
        
    }
    
}