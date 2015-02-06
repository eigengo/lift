import Foundation
import XCTest

class SensorDataGroupBufferTests : XCTestCase {
    
    class TestSenorDataGroupBufferDelegate : SensorDataGroupBufferDelegate {
        
        var decodedData: [UInt8 : [UInt8]] = [:]
        
        func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedAt time: CFAbsoluteTime, data: NSData) {
            NSLog("**** Got %@", data)
            
            collectData(data)
        }
        
        func collectData(data: NSData) -> Void {
            let bytes = data.asByteVector()
            let packetCount = bytes.drop(2).readUInt8()
            bytes.drop(4)
            
            for i in 0..<packetCount {
                let size = bytes.readUInt16()
                let location = UInt8(bytes.readUInt8())
                let dataArray = bytes.drop(5).readByteArray(size - 5)
                appendDecodedData(location, input: dataArray)
                
                NSLog("\(i + 1) of \(packetCount): size = \(size), location = \(location), data = \(dataArray)")
            }
        }
        
        func appendDecodedData(location: UInt8, input: [UInt8]) -> Void {
            if var current = decodedData[location] {
                current += input
                decodedData[location] = current
            } else {
                decodedData[location] = input
            }
        }
        
    }
    
    func startsWith(substring: [UInt8], _ input: [UInt8]) -> Bool {
        if (substring.count > input.count) {
            return false
        }
        
        for i in [0..<substring.count] {
            if substring[i] != input[i] {
                return false
            }
        }
        
        return true
    }
    
    func testContinuous() {
        self.continueAfterFailure = false
        
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
        
        if let decodedPhoneData = delegate.decodedData[DeviceInfo.Location.Waist.rawValue] {
            XCTAssertTrue(startsWith([0x41, 0x42, 0x31, 0x32, 0x61, 0x62, 0x61, 0x63], decodedPhoneData), "Phone data did not arrive.")
        } else {
            XCTFail("Phone data has not arrived!")
            return
        }
        
        if let decodedPhoneData = delegate.decodedData[DeviceInfo.Location.Wrist.rawValue] {
            XCTAssertTrue(startsWith([0x23, 0x24], decodedPhoneData), "Pebble data did not arrive.")
        } else {
            XCTFail("Pebble data has not arrived!")
            return
        }
        
        buf.decodeAndAdd(TestSensorData.phoneData, fromDeviceId: TestSensorData.phone, maximumGap: 0.3, gapValue: 0)
        sleep(1)
        buf.stop()
    }
    
}