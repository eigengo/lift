import Foundation

class ContinuousSensorDataArrayTests : XCTestCase {
    
    class func dummyHeader() -> SensorDataArrayHeader {
        return SensorDataArrayHeader(sourceDeviceId: DeviceId(), type: 0x7f, sampleSize: 1, samplesPerSecond: 1)
    }
    
    class func createSensorData(bytes: [UInt8]) -> SensorData {
        let samples = NSMutableData(bytes: bytes, length: bytes.count)
        return SensorData(startTime: CFAbsoluteTimeGetCurrent(), samples: samples)
    }
    
    class func createContinuousArray(data: SensorData) -> ContinuousSensorDataArray {
        return ContinuousSensorDataArray(header: ContinuousSensorDataArrayTests.dummyHeader(), sensorData: data)
    }
    
    //    func encode(typeByte: UInt8, bytes: [UInt8]) -> [UInt8] {
    //        var mutableData = NSMutableData()
    //        let continuousArray: ContinuousSensorDataArray =
    //            ContinuousSensorDataArrayTests.createContinuousArray(
    //                ContinuousSensorDataArrayTests.createSensorData( bytes ) )
    //        continuousArray.encode(mutating: mutableData)
    //
    //        return mutableData.extractBytes()
    //    }
    //
    //    func testEncoding() {
    //        XCTAssertEqual(encode(0x7f, bytes: [0x01, 0x02]), [0x02, 0x00, 0x7f, 0x01, 0x02])
    //    }
    
}