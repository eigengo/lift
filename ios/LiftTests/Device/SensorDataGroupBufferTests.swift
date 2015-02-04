import Foundation
import XCTest

class SensorDataGroupBufferTests : XCTestCase {
    
    func testEmptyDataEncoding() {
        let emptySlice = Slice<ContinuousSensorDataArray>()
        let encodedData = SensorDataGroupBuffer.createEncodedData(emptySlice, timeStamp: 2882400000.0)
        XCTAssertArraysEqual(encodedData.extractBytes(), [0xb1, 0xca, 0x00, 0x00, 0xef, 0xcd, 0xab])
    }
    
    func testDataEncoding() {
        
        let arrays =
            [
                ContinuousSensorDataArrayTests.createContinuousArray(
                    ContinuousSensorDataArrayTests.createSensorData([0xda, 0x1a])),
                ContinuousSensorDataArrayTests.createContinuousArray(
                    ContinuousSensorDataArrayTests.createSensorData([0xca, 0x7e, 0x11]))
            ][0...1]
        
        let encodedData = SensorDataGroupBuffer.createEncodedData(arrays, timeStamp: 2018915346.0)
        XCTAssertArraysEqual(encodedData.extractBytes(),
            [
                0xb1, 0xca, 0x02, 0x12, 0x34, 0x56, 0x78,           // header, count, timestamp
                0x02, 0x00, 0x7f, 0xda, 0x1a,                       // series 1
                0x03, 0x00, 0x7f, 0xca, 0x7e, 0x11                  // series 2
            ])
    }
    
}