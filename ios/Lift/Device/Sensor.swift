import Foundation

///
/// The device stats keys
///
enum SensorKind : Hashable {
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
    
    var hashValue: Int {
        get {
            switch self {
            case .Accelerometer: return 1
            case .Gyroscope: return 2
            case .GPS: return 3
            case .HeartRate: return 4
            case .Other(let x): return 21 * Int(x)
            }
        }
    }
}

func ==(lhs: SensorKind, rhs: SensorKind) -> Bool {
    switch (lhs, rhs) {
    case (.Accelerometer, .Accelerometer): return true
    case (.Gyroscope, .Gyroscope): return true
    case (.GPS, .GPS): return true
    case (.HeartRate, .HeartRate): return true
    case (.Other(let l), .Other(let r)): return l == r
        
    default: return false
    }
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
    var sensorDataArrays: [SensorDataArrayHeader : SensorDataArray] = [:]
    
    ///
    /// Adds raw ``data`` received from device ``id`` at some ``time``
    ///
    mutating func decodeAndAdd(data: NSData, fromDeviceId id: DeviceId, at time: CFAbsoluteTime) -> Void {
        var header: lift_header?
        data.getBytes(&header, length: sizeof(lift_header))
        
        let key = SensorDataArrayHeader(deviceId: id, header: header!)
        let samples = data.subdataWithRange(NSMakeRange(sizeof(lift_header), data.length - sizeof(lift_header)))
        let sensorData = SensorData(startTime: time, samples: samples)
        if var x = sensorDataArrays[key] {
            x.addSensorData(sensorData)
        } else {
            sensorDataArrays[key] = SensorDataArray(header: key, sensorData: sensorData)
        }
    }
    
}

///
/// The header for sensor data. It roughly matches the ``lift_header`` C struct.
///
struct SensorDataArrayHeader : Hashable {
    /// the source deviceId
    var sourceDeviceId: DeviceId
    /// the type of data
    var type: UInt8
    /// the sample size
    var sampleSize: UInt8
    /// the number of samples per second
    var samplesPerSecond: UInt8
    
    init(deviceId: DeviceId, header: lift_header) {
        sourceDeviceId = deviceId
        type = header.type
        sampleSize = header.sample_size
        samplesPerSecond = header.samples_per_second
    }
    
    var hashValue: Int {
        get {
            return sourceDeviceId.hashValue +
                31 * Int(type) +
                31 * Int(sampleSize) +
                31 * Int(samplesPerSecond)
        }
    }
}

func ==(lhs: SensorDataArrayHeader, rhs: SensorDataArrayHeader) -> Bool {
    return lhs.sourceDeviceId == rhs.sourceDeviceId &&
           lhs.type == rhs.type &&
           lhs.sampleSize == rhs.sampleSize &&
           lhs.samplesPerSecond == rhs.samplesPerSecond
}

///
/// Potentially gappy SDA groups a number of ``SensorData`` structures with the same ``header``.
///
struct SensorDataArray {
    /// the header
    var header: SensorDataArrayHeader
    /// the data
    var sensorDatas: [SensorData]
    
    init(header: SensorDataArrayHeader, sensorData: SensorData) {
        self.header = header
        sensorDatas = [sensorData]
    }
    
    ///
    /// Adds another ``sensorData``
    ///
    mutating func addSensorData(sensorData: SensorData) {
        sensorDatas += [sensorData]
    }
    
    ///
    /// Computes the continuous ranges of as an array of ``TimeRange``,
    /// "compacting" the sensor data that are less than ``maximumGap`` apart.
    ///
    func continuousRanges(maximumGap: CFTimeInterval) -> [TimeRange] {
        if sensorDatas.isEmpty { return [] }
        
        var result: [TimeRange] = []
        var time = sensorDatas.first!.startTime
        var blockStartTime = time
        for sensorData in sensorDatas {
            if abs(sensorData.startTime - time) < maximumGap {
                // the start time of the next block is within maximumGap
                // we don't accumulate the gaps
                time = sensorData.startTime
                // increase the time to the end of the current block
                time += sensorData.duration(header.sampleSize, samplesPerSecond: header.samplesPerSecond)
            } else {
                // the start time of the next block is outside maximumGap.
                // we have range from blockStartTime to time
                result += [TimeRange(start: blockStartTime, end: time)]
                // reset our running counters
                blockStartTime = sensorData.startTime
                time = sensorData.startTime
            }
        }
        
        return result
    }
    
    func continuousSampleIn(maximumGap: CFTimeInterval, range: TimeRange) -> SensorData? {
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
    
    ///
    /// Computes the end time of the sensor data block given the
    /// ``sampleSize`` and ``samplesPerSecond``
    ///
    func duration(sampleSize: UInt8, samplesPerSecond: UInt8) -> CFTimeInterval {
        let sampleCount = samples.length / Int(sampleSize)
        return CFTimeInterval(sampleCount / Int(samplesPerSecond))
    }
}
