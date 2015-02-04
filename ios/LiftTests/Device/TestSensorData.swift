import Foundation

///
/// Sample test data
///
struct TestSensorData {
    /// Phone device id
    static let phone = DeviceId()
    /// Sample data from the phone
    static let phoneData: NSData = {
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
    
    /// Pebble device id
    static let pebble = DeviceId()
    /// Sample data from the pebble
    static let pebbleData: NSData = {
        let buffer: [UInt8] = [
            0x00, 0x02, 0x02, 0x01, 0x00,  // type: 0, count: 2, samplesPerSecond: 2, sampleSize: 1, _: 0
            0x23, 0x24                     // '#', '$'
        ]
        // data represents 3 sensor data arrays, each 1s long
        return NSData(bytes: buffer, length: buffer.count)
        }()

}