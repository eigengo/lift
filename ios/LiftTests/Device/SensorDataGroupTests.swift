import Foundation
import XCTest

class SensorDataGroupTests : XCTestCase {
    let deviceId = DeviceId()

    let simpleTestData: NSData = {
        let buffer: [UInt8] = [
            0x00, 0x02, 0x02, 0x01, 0x00,  // type: 0, count: 2, samplesPerSecond: 2, sampleSize: 1, _: 0
            0x41, 0x42,                    // 'A', 'B'
            0x01, 0x02, 0x02, 0x01, 0x00,  // type: 1, count: 2, samplesPerSecond: 2, sampleSize: 1, _: 0
            0x31, 0x32,                    // '1', '2'
            0x02, 0x02, 0x02, 0x02, 0x00,  // type: 2, count: 2, samplesPerSecond: 2, sampleSize: 2, _: 0
            0x61, 0x62, 0x61, 0x63         // 'a'~'b', 'a'~'c'
        ]
        // data represents 3 sensor data arrays, each 1s long
        return NSData(bytes: buffer, length: buffer.count)
    }()

    func testSimpleContinuous() {
        var sdg = SensorDataGroup()
        
        // 3 things, all starting at 1, 2 samples, 1 sps.
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 1)
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 2)     // we're within our gap of 0.1 ~> inline merges

        let csdas12 = sdg.continuousSensorDataArrays(within: TimeRange(start: 1, end: 2), maximumGap: 0, gapValue: 0)
        XCTAssertEqual(csdas12.find { $0.header.type == 0 }!.sensorData.asString(), "AB")
        XCTAssertEqual(csdas12.find { $0.header.type == 1 }!.sensorData.asString(), "12")
        XCTAssertEqual(csdas12.find { $0.header.type == 2 }!.sensorData.asString(), "abac")
        
        
        let csdas13 = sdg.continuousSensorDataArrays(within: TimeRange(start: 1, end: 3), maximumGap: 0, gapValue: 0)
        XCTAssertEqual(csdas13.find { $0.header.type == 0 }!.sensorData.asString(), "ABAB")
        XCTAssertEqual(csdas13.find { $0.header.type == 1 }!.sensorData.asString(), "1212")
        XCTAssertEqual(csdas13.find { $0.header.type == 2 }!.sensorData.asString(), "abacabac")
    }
    
    func testSimpleWithinGaps() {
        var sdg = SensorDataGroup()
        
        // 3 things, all starting at 1, 2 samples, 1 sps.
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 1)
        sdg.decodeAndAdd(simpleTestData, fromDeviceId: deviceId, at: 2.5)   // we're over our gap of 0.1 ~> no inline merges

        // no continuous ranges with gap == 0
        XCTAssertTrue(sdg.continuousSensorDataArrays(within: TimeRange(start: 1, end: 3.5), maximumGap: 0, gapValue: 0).isEmpty)
        
        let csdas = sdg.continuousSensorDataArrays(within: TimeRange(start: 1, end: 3.5), maximumGap: 0.5, gapValue: 0)
        XCTAssertEqual(csdas.find { $0.header.type == 0 }!.sensorData.asString(), "ABAB")
        XCTAssertEqual(csdas.find { $0.header.type == 1 }!.sensorData.asString(), "1212")
        XCTAssertEqual(csdas.find { $0.header.type == 2 }!.sensorData.asString(), "abacabac")
    }
    
}