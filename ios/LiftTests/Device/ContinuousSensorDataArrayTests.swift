import Foundation
import XCTest

class ContinuousSensorDataArrayTests : XCTestCase {
        
    func testEncode() {
        let header = SensorDataArrayHeader(sourceDeviceId: TestSensorData.phone, type: 0xff, sampleSize: 0x01, samplesPerSecond: 0xff)
        let data = "ABC".dataUsingEncoding(NSASCIIStringEncoding, allowLossyConversion: false)!
        let cda = ContinuousSensorDataArray(header: header, sensorData: SensorData(startTime: 0, samples: data))
        
        let result = NSMutableData()
        cda.encode(mutating: result)
        let bytes = result.asBytes()

        XCTAssertEqual(bytes[0], header.type)
        XCTAssertEqual(bytes[1], UInt8(3))
        XCTAssertEqual(bytes[2], header.samplesPerSecond)
        XCTAssertEqual(bytes[3], header.sampleSize)
        // 4 is padding
        XCTAssertEqual(bytes[5], UInt8(0x41))   // A
        XCTAssertEqual(bytes[6], UInt8(0x42))   // B
        XCTAssertEqual(bytes[7], UInt8(0x43))   // C
    }
    
}