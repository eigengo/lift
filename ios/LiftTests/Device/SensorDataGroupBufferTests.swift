import Foundation
import XCTest

class SensorDataGroupBufferTests : XCTestCase {
    
    class TestSenorDataGroupBufferDelegate : SensorDataGroupBufferDelegate {
        var data: NSData?
        
        func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedAt time: CFAbsoluteTime, data: NSData) {
            NSLog("**** Got %@", data)
            self.data = data
        }
    }
    
    func testContinuous() {
        let delegate = TestSenorDataGroupBufferDelegate()
        let buf = SensorDataGroupBuffer(delegate: delegate, queue: sensorDataGroupBufferQueue, deviceLocations: TestSensorData.deviceLocations)
        buf.decodeAndAdd(TestSensorData.phoneData,  fromDeviceId: TestSensorData.phone,  maximumGap: 0.3, gapValue: 0)
        buf.decodeAndAdd(TestSensorData.pebbleData, fromDeviceId: TestSensorData.pebble, maximumGap: 0.3, gapValue: 0)
        sleep(1)
        buf.decodeAndAdd(TestSensorData.phoneData,  fromDeviceId: TestSensorData.phone,  maximumGap: 0.3, gapValue: 0)
        buf.decodeAndAdd(TestSensorData.pebbleData, fromDeviceId: TestSensorData.pebble, maximumGap: 0.3, gapValue: 0)
        sleep(1)
        buf.decodeAndAdd(TestSensorData.phoneData,  fromDeviceId: TestSensorData.phone,  maximumGap: 0.3, gapValue: 0)
        buf.decodeAndAdd(TestSensorData.pebbleData, fromDeviceId: TestSensorData.pebble, maximumGap: 0.3, gapValue: 0)
        sleep(1)
        // TODO: assert data
        //
        // cab1 03 00000002
        // ^    ^  ^
        // |    |  |
        // |    |  timestamp
        // |    count
        // | header
        // 
        // 00 07 7f 0002 02010041 4200077f 01020201 00313200 097f0202 02020061 626163 
        // ???
        //
        XCTAssertTrue(delegate.data != nil)
        buf.decodeAndAdd(TestSensorData.phoneData, fromDeviceId: TestSensorData.phone, maximumGap: 0.3, gapValue: 0)
        sleep(1)
        buf.stop()
    }
    
}