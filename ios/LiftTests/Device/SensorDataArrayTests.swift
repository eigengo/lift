import Foundation
import XCTest

class SensorDataArrayTests : XCTestCase {
    
    func testContinuousRanges() {
        var sda = SensorDataArray(header: SensorDataArrayHeader(sourceDeviceId: DeviceId(), type: 0, sampleSize: 1, samplesPerSecond: 1))
        sda.append(sensorData: SensorData.fromString("abcdefghij", startingAt: 0), maximumGap: 0, gapValue: 0)       // 0 - 10
        sda.append(sensorData: SensorData.fromString("0123456789", startingAt: 11), maximumGap: 0, gapValue: 0)      // 11 - 21
        sda.append(sensorData: SensorData.fromString("ABCDEFGHIJ", startingAt: 22), maximumGap: 0, gapValue: 0)      // 22 - 32
        
        let cr = sda.continuousRanges(maximumGap: 1)
        XCTAssertEqual(cr.count, 1)
        XCTAssertEqual(cr[0].start, 0); XCTAssertEqual(cr[0].end, 32)

        let crC = sda.continuousRanges(maximumGap: 100)
        XCTAssertEqual(crC.count, 1)
        XCTAssertEqual(crC[0].start, 0); XCTAssertEqual(crC[0].end, 32)

        let dr = sda.continuousRanges(maximumGap: 0)
        XCTAssertEqual(dr.count, 3)
        XCTAssertEqual(dr[0].start, 0); XCTAssertEqual(dr[0].end, 10)
        XCTAssertEqual(dr[1].start, 11); XCTAssertEqual(dr[1].end, 21)
        XCTAssertEqual(dr[2].start, 22); XCTAssertEqual(dr[2].end, 32)
    }

}

