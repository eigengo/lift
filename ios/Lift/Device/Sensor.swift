import Foundation

///
/// The device stats keys
///
enum SensorKind {
    /// Accelerometer 0xad
    case Accelerometer
    /// Gyroscope     0xbd
    case Gyroscope
    /// GPS           0xcd
    case GPS
    /// HR            0xdd
    case HeartRate
    /// Some other type
    case Other(type: UInt8)
}

///
/// The time range
///
struct TimeRange {
    var start: CFAbsoluteTime
    var end: CFAbsoluteTime
    
    ///
    /// Intersection of ``self`` and ``that``, returning a ``TimeRange`` that contains both
    /// self and that.
    ///
    func intersect(that: TimeRange) -> TimeRange {
        fatalError("Implement me")
    }
}

///
/// Groups multiple ``SensorDataArray``s, typically received from multiple devices
///
struct SensorDataGroup {
    var sensorDataArrays: [SensorDataArray] = []
    
    mutating func addSensorDataArray(sda: SensorDataArray) {
        sensorDataArrays += [sda]
    }
    
    func sensorDataArraysWith(maximumGap: CFTimeInterval) -> [ContinuousSensorDataArray] {
        fatalError("Implement me")
    }
}

///
/// The header for sensor data. It roughly matches the ``lift_header`` C struct.
///
struct SensorDataArrayHeader {
    /// the type of data
    var type: UInt8
    /// the sample size
    var sampleSize: UInt8
    /// the number of samples per second
    var samplesPerSecond: UInt8
}

///
/// Continuous SDA has one single block of ``sensorData`` with the ``header``
///
struct ContinuousSensorDataArray {
    var header: SensorDataArrayHeader
    var sensorData: SensorData
}

///
/// Potentially gappy SDA groups a number of ``SensorData`` structures with the same ``header``.
///
struct SensorDataArray {
    /// the header
    var header: SensorDataArrayHeader
    /// the data
    var sensorData: [SensorData]
    
    func continuousRanges(from: CFAbsoluteTime) -> [TimeRange] {
        fatalError("Implement me")
    }
    
    func continuousSampleIn(range: TimeRange) -> ContinuousSensorDataArray? {
        fatalError("Implement me")
    }
}

///
/// The single SensorData is the payload from the sensor received at the given ``startTime``.
///
struct SensorData {
    /// the time the block of samples was received
    var startTime: CFAbsoluteTime
    /// the samples
    var samples: NSData
}
