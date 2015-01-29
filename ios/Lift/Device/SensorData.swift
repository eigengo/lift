import Foundation

infix operator =~= { associativity left precedence 140 }
func =~=(lhs: Double, rhs: Double) -> Bool {
    let epsilon = 0.0001
    return abs(lhs - rhs) < epsilon
}

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
struct TimeRange : Equatable {
    var start: CFAbsoluteTime
    var end: CFAbsoluteTime
    
    ///
    /// Intersection of ``self`` and ``that``, returning a ``TimeRange`` that contains both
    /// self and that.
    ///
    func intersect(that: TimeRange) -> TimeRange {
        fatalError("Implement me")
    }
    
    ///
    /// Indicates that this ``TimeRange`` is within ``that``, allowing for some ``gap``
    ///
    func within(that: TimeRange, maximumGap gap: CFTimeInterval) -> Bool {
        return abs(start - that.start) <= gap && abs(end - that.end) <= gap
    }
}

func ==(lhs: TimeRange, rhs: TimeRange) -> Bool {
    return lhs.start =~= rhs.start && lhs.end =~= rhs.end
}

///
/// Groups multiple ``SensorDataArray``s, typically received from multiple devices
///
struct SensorDataGroup {
    var sensorDataArrays: [SensorDataArrayHeader : SensorDataArray] = [:]
    
    private mutating func decode(input: NSData?, fromDeviceId id: DeviceId, at time: CFAbsoluteTime, maximumGap gap: CFTimeInterval, gapValue: UInt8) -> Void {
        if let data = input {
            var header: lift_header?
            data.getBytes(&header, length: sizeof(lift_header))
            
            let key = SensorDataArrayHeader(sourceDeviceId: id, type: header!.type, sampleSize: header!.sample_size, samplesPerSecond: header!.samples_per_second)
            let length = Int(header!.count * header!.sample_size)
            let samples = data.subdataWithRange(NSMakeRange(sizeof(lift_header), length))
            let sensorData = SensorData(startTime: time, samples: samples)
            if var x = sensorDataArrays[key] {
                x.append(sensorData: sensorData, maximumGap: gap, gapValue: gapValue)
            } else {
                sensorDataArrays[key] = SensorDataArray(header: key, sensorData: sensorData)
            }
            
            if data.length > samples.length + sizeof(lift_header) {
                let zero = sizeof(lift_header) + samples.length
                decode(data.subdataWithRange(NSMakeRange(zero, data.length - zero)), fromDeviceId: id, at: time, maximumGap: gap, gapValue: gapValue)
            }
        }
    }
    
    ///
    /// The number of raw SDAs
    ///
    var rawCount: Int {
        get {
            return sensorDataArrays.count
        }
    }

    ///
    /// Adds raw ``data`` received from device ``id`` at some ``time``. The ``data`` may result in multiple
    /// ``SensorDataArray``s being added: in one packet, a device may send—for example—the accelerometer
    /// data *and* the heart rate data. 
    ///
    /// It is sensible for the device to do so: it saves power by allowing the device to keep its radio
    /// usage low, but more importantly, it makes it easier for the device to maintain time synchronization
    /// in the values it sends.
    ///
    /// It will also perform trivial merging if the last received data block arrives within ``gap`` of the last
    /// received data block.
    ///
    mutating func decodeAndAdd(data: NSData, fromDeviceId id: DeviceId, at time: CFAbsoluteTime,
        maximumGap gap: CFTimeInterval = 0.1, gapValue: UInt8 = 0x00) -> Void {
        
        decode(data, fromDeviceId: id, at: time, maximumGap: gap, gapValue: gapValue)
    }
    
    ///
    /// Removes ``SensorDataArray``s whose endDate falls before the given ``time``.
    ///
    mutating func removeSensorDataArraysEndingBefore(time: CFAbsoluteTime) -> Void {
        for (_, var v) in sensorDataArrays {
            v.removeSensorDataEndingBefore(time)
        }
    }

    ///
    /// Computes the continuous ``SensorDataArray``s
    ///
    func continuousSensorDataArrays(maximumGap gap: CFTimeInterval, gapValue: UInt8, maximumDuration: CFTimeInterval) -> [SensorDataArray] {
        
        /// returns ``TimeRange`` if the elements in ``tras`` all contain exactly 1 ``TimeRange``, and this ``TimeRange`` is the same
        func singleTimeRange(tras: [[TimeRange]]) -> TimeRange? {
            if tras.isEmpty { return nil }
            if tras[0].isEmpty || tras[0].count > 1 { return nil }
            
            var firstTr: TimeRange = tras[0][0]
            
            for trs in tras {
                for tr in trs {
                    if tr != firstTr { return nil }
                }
            }
            
            return firstTr
        }
        
        if sensorDataArrays.isEmpty { return [] }
        
        let sdas = sensorDataArrays.values.array
        
        if let x = singleTimeRange(sdas.map { $0.continuousRanges(maximumGap: gap) }) {
            return []
        }
        return []
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
    
    init(header: SensorDataArrayHeader) {
        self.header = header
        sensorDatas = []
    }
    
    init(header: SensorDataArrayHeader, sensorData: SensorData) {
        self.header = header
        sensorDatas = [sensorData]
    }
    
    ///
    /// Adds another ``sensorData``. It will also perform trivial merging: that is, if the ``sensorData`` 
    /// being added follows immediately (for some very small epsilon) the last ``sensorData``, then it
    /// will just be appended to the last ``sensorData``.
    ///
    mutating func append(sensorData data: SensorData, maximumGap gap: CFTimeInterval, gapValue: UInt8) {
        if var last = sensorDatas.last {
            if data.startTime - last.endTime(header.sampleSize, samplesPerSecond: header.samplesPerSecond) < gap {
                last.padEnd(header.sampleSize, samplesPerSecond: header.samplesPerSecond, gapValue: gapValue, until: data.startTime)
                last.append(samples: data.samples)
                return
            }
        }
        sensorDatas += [data]
    }
    
    ///
    /// Removes ``SensorData`` elements whose end time is before ``time``
    ///
    mutating func removeSensorDataEndingBefore(time: CFAbsoluteTime) -> Void {
        sensorDatas = sensorDatas.filter { sd in
            let endTime = sd.endTime(self.header.sampleSize, samplesPerSecond: self.header.samplesPerSecond)
            return endTime < time
        }
    }
    
    func slice(range: TimeRange, maximumGap gap: CFTimeInterval, gapValue: UInt8) -> [SensorData] {
        return []
    }
    
    ///
    /// Computes the continuous ranges of as an array of ``TimeRange``,
    /// "compacting" the sensor data that are less than ``maximumGap`` apart.
    ///
    func continuousRanges(maximumGap gap: CFTimeInterval) -> [TimeRange] {
        if sensorDatas.isEmpty { return [] }
        
        var result: [TimeRange] = []
        var time = sensorDatas.first!.startTime

        for (i, sensorData) in enumerate(sensorDatas) {
            let endTime = sensorData.endTime(header.sampleSize, samplesPerSecond: header.samplesPerSecond)
            if (i + 1) < sensorDatas.count {
                // there is an element after this one: does it start within the maximumGap?
                let nextSensorData: SensorData = sensorDatas[i + 1]
                // [ SD ]  [ NSD ]
                //      ^  ^
                if nextSensorData.startTime - endTime > gap {
                    // no: we have another TimeRange
                    result += [TimeRange(start: time, end: endTime)]
                    time = nextSensorData.startTime
                }
            } else {
                result += [TimeRange(start: time, end: endTime)]
            }
        }
        
        return result
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
    
    /// Append gap of given ``length`` filled with ``gapValue`` to the ``data``
    private func appendGap(length: Int, gapValue value: UInt8, toData data: NSMutableData) -> Void {
        let buf: [UInt8] = [UInt8](count: length, repeatedValue: value)
        data.appendBytes(buf, length: length)
    }

    ///
    /// Append another ``data`` to the ``samples``
    ///
    mutating func append(samples data: NSData) {
        let x = NSMutableData(data: samples)
        x.appendData(data)
        samples = x
    }
    
    ///
    /// Pads end of this ``SensorData`` with ``gapValue`` until its end time matches ``until``.
    ///
    mutating func padEnd(sampleSize: UInt8, samplesPerSecond: UInt8, gapValue: UInt8, until: CFTimeInterval) {
        let endTime = startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
        let endGap = until - endTime
        if endGap > 0 {
            let length = Int( endGap * Double(samplesPerSecond) * Double(sampleSize) )
            let x = NSMutableData(data: samples)
            appendGap(length, gapValue: gapValue, toData: x)
            samples = x
        }
    }
    
    ///
    /// Computes the end time of the sensor data block given the
    /// ``sampleSize`` and ``samplesPerSecond``
    ///
    func duration(sampleSize: UInt8, samplesPerSecond: UInt8) -> CFTimeInterval {
        let sampleCount = samples.length / Int(sampleSize)
        return CFTimeInterval(sampleCount / Int(samplesPerSecond))
    }
    
    ///
    /// Computes the end time of the sensor data block given the
    /// ``sampleSize`` and ``samplesPerSecond``
    ///
    func endTime(sampleSize: UInt8, samplesPerSecond: UInt8) -> CFAbsoluteTime {
        return startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
    }
    
    ///
    /// Computes whether this slice is within the given range
    ///
    func within(range: TimeRange, maximumGap gap: CFTimeInterval, sampleSize: UInt8, samplesPerSecond: UInt8) -> Bool {
        let endTime = startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
        let startGap = startTime - range.start
        let endGap = range.end - endTime
        
        return startGap <= gap && endGap <= gap
    }
    
    ///
    /// Computes slice of samples that fall within the given time range, 
    /// allowing for up to ``maximumGap`` before and after this data.
    /// Returns the ``SensorData`` with appropriately set ``startTime`` if
    /// the ``range`` is valid, or ``nil``.
    ///
    ///                         startTime + duration + maximumGap |
    ///                                                           |
    ///                                    startTime + duration | |
    ///                                                         v v
    /// [__######################################################__]
    ///  ^ ^
    ///  | | startTime
    ///  |
    ///  | startTime - maximumGap
    ///
    func slice(range: TimeRange, maximumGap gap: CFTimeInterval, sampleSize: UInt8, samplesPerSecond: UInt8, gapValue: UInt8) -> SensorData? {
        let endTime = startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
        let startGap = startTime - range.start
        let endGap = range.end - endTime
        
        if startGap > gap || endGap > gap { return nil }

        // start and length are in bytes: pointers to our ``sample``
        let start  = Int( -startGap * Double(samplesPerSecond) * Double(sampleSize) )
        let length = Int( (endTime + startGap + endGap) * Double(samplesPerSecond) * Double(sampleSize) )

        if start == 0 && length == samples.length {
            // Exact match. Notice that we do the comparison here on Ints rather than the CFAbsoluteTimes above.
            return self
        }
        
        if start > 0 && (start + length) < samples.length {
            // Range is completely within our data: no gaps
            return SensorData(startTime: startTime - startGap, samples: samples.subdataWithRange(NSMakeRange(start, length)))
        }
        
        // Allowable gaps
        let r = NSMutableData()
        if start < 0 { appendGap(-start, gapValue: gapValue, toData: r) }
        let l = min(length + start, samples.length) - max(start, 0)
        r.appendData(samples.subdataWithRange(NSMakeRange(max(start, 0), l)))
        if r.length < length {
            appendGap(length - r.length, gapValue: gapValue, toData: r)
        }
        return SensorData(startTime: startTime - startGap, samples: r)
    }
}
