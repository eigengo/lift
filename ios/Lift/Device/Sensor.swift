import Foundation

///
/// The device stats keys
///
enum SensorKind {
    case Accelerometer
    case Gyroscope
    case GPS
    case HeartRate
}

struct SensorData {
    var type: UInt8
    var count: UInt8
    var samplesPerSecond: UInt8
    var last: UInt16
    var startTime: CFAbsoluteTime
    var data: NSData
    
    
}