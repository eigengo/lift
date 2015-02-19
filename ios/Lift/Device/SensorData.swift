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
    /// Computers the length of this interval
    ///
    var length: CFTimeInterval {
        get {
            return end - start
        }
    }
    
    ///
    /// Indicates that this ``TimeRange`` is within ``that``, allowing for some ``gap``
    ///
    func within(that: TimeRange, maximumGap gap: CFTimeInterval) -> Bool {
        return (start - gap) >= that.start && (end + gap) <= that.end
    }
    
    ///
    /// Indicates whether the given value is contained in this range.
    ///
    func contains(value: CFAbsoluteTime) -> Bool {
        return (start <= value) && (value <= end)
    }
    
    ///
    /// Returns true if the TimeRange ``other`` contains any common time point with this instance.
    ///
    func intersects(other: TimeRange) -> Bool {
        return ((other.start >= start) && (other.start <= end)) || ((start >= other.start) && (start <= other.end))
    }
}

func ==(lhs: TimeRange, rhs: TimeRange) -> Bool {
    return lhs.start =~= rhs.start && lhs.end =~= rhs.end
}

///
/// Groups multiple ``SensorDataArray``s, typically received from multiple devices
///
class SensorDataGroup {
    var sensorDataArrays: [SensorDataArray] = []
    
    private func decode(data: NSData, fromDeviceId id: DeviceId, at time: CFAbsoluteTime, maximumGap gap: CFTimeInterval, gapValue: UInt8) -> Void {
        let headerSize = 5
        var header: [UInt8] = [ 0, 0, 0, 0, 0 ]
        if data.length > headerSize {
            data.getBytes(&header, length: headerSize)
            let type = header[0]
            let count = header[1]
            let samplesPerSecond = header[2]
            let sampleSize = header[3]

            let length = Int(count) * Int(sampleSize)
            if data.length < headerSize + length {
                fatalError("Received \(data.length) bytes, expected \(headerSize + length)")
            }
            let key = SensorDataArrayHeader(sourceDeviceId: id, type: type, sampleSize: sampleSize, samplesPerSecond: samplesPerSecond)
            let samples = data.subdataWithRange(NSMakeRange(headerSize, length))
            let sensorData = SensorData(startTime: time, samples: samples)
            if let x = (sensorDataArrays.find { $0.header == key }) {
                x.append(sensorData: sensorData, maximumGap: gap, gapValue: gapValue)
            } else {
                sensorDataArrays += [SensorDataArray(header: key, sensorData: sensorData)]
            }
            
            if data.length > samples.length + headerSize {
                let zero = headerSize + samples.length
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
    /// The length of all samples in B
    ///
    var length: Int {
        get {
            var r: Int = 0
            for x in sensorDataArrays { r += x.length }
            return r
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
    /* mutating */
    func decodeAndAdd(data: NSData, fromDeviceId id: DeviceId, at time: CFAbsoluteTime,
        maximumGap gap: CFTimeInterval = 0.1, gapValue: UInt8 = 0x00) -> Void {
        //objc_sync_enter(SensorDataGroup.lock)
        decode(data, fromDeviceId: id, at: time, maximumGap: gap, gapValue: gapValue)
        //objc_sync_exit(SensorDataGroup.lock)
    }
    
    ///
    /// Removes ``SensorDataArray``s whose endDate falls before the given ``time``.
    ///
    /* mutating */
    func removeSensorDataArraysEndingBefore(time: CFAbsoluteTime) -> Void {
        for v in sensorDataArrays {
            v.removeSensorDataEndingBefore(time)
        }
    }
    
    ///
    /// Computes the continuous ``SensorDataArray``s
    ///
    func continuousSensorDataArrays(within range: TimeRange, maximumGap gap: CFTimeInterval, gapValue: UInt8) -> [ContinuousSensorDataArray] {
        if sensorDataArrays.isEmpty { return [] }

        var result: [ContinuousSensorDataArray] = []
        for sda in sensorDataArrays {
            if let x = sda.slice(range, maximumGap: gap, gapValue: gapValue) {
                result += [x]
            }
        }
        return result
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
/// A SensorDataArray that only holds continuous ``sensorData``. Note that the ``sensorData`` here
/// could be padded
///
struct ContinuousSensorDataArray {
    /// the header
    var header: SensorDataArrayHeader
    /// the continuous (possibly padded) SensorData
    var sensorData: SensorData
    
    ///
    /// Encodes the date here into the form that was received from the server.
    /// 
    /// Given
    ///
    /// input = [type,...,|#########], that is some bytes of the given ``type``
    /// received from device with ``deviceId``
    /// 
    /// When
    /// 
    /// result = NSMutableData()
    /// sdg = SensorDataGroup()
    /// sdg.decodeAndAdd(input, ...)
    /// sdg.continuousSensorDataArrays(...)
    ///    .find { $0.header.sourceDeviceId == deviceId && $0.header.type == type }!
    ///    .encode(mutating: result)
    /// 
    /// Then
    ///
    /// result = input
    ///
    func encode(mutating data: NSMutableData) -> Void {
        let c = sensorData.samples.length / Int(header.sampleSize)
        if c > 255 { fatalError("Too many samples. Consider changing count to be uint16_t.") }
        let count = UInt8(c)
        data.appendUInt8(header.type)
        data.appendUInt8(count)
        data.appendUInt8(header.samplesPerSecond)
        data.appendUInt8(header.sampleSize)
        data.appendUInt8(0)
        
        data.appendData(sensorData.samples)
        
    }
    
    ///
    /// Computes the entire length of this CSDA. This is the number of 
    /// bytes that will be appended when calling the ``encode(mutating:)`` method
    ///
    var length: Int {
        get {
            // We *know* that the header is 5B long
            return 5 + sensorData.samples.length
        }
    }
}

///
/// Potentially gappy SDA groups a number of ``SensorData`` structures with the same ``header``.
///
class SensorDataArray {
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
    /// The size in B of all SensorData
    ///
    var length: Int {
        get {
            var result: Int = 0
            for sd in sensorDatas {
                result += sd.samples.length
            }
            return result
        }
    }
    
    ///
    /// Adds another ``sensorData``. It will also perform trivial merging: that is, if the ``sensorData`` 
    /// being added follows immediately (for some very small epsilon) the last ``sensorData``, then it
    /// will just be appended to the last ``sensorData``.
    ///
    /* mutating */
    func append(sensorData data: SensorData, maximumGap gap: CFTimeInterval, gapValue: UInt8) {
        if var last = sensorDatas.last {
            let currentStart = data.startTime
            let lastEnd = last.endTime(header.sampleSize, samplesPerSecond: header.samplesPerSecond)
            if currentStart < lastEnd {
                let bytesToRemove = Int(ceil(Double(lastEnd - currentStart) * Double(header.samplesPerSecond))) * Int(header.sampleSize)
                last.removeFromEnd(bytesToRemove)
                last.append(samples: data.samples)
                return
            } else if currentStart - lastEnd < gap {
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
    /* mutating */
    func removeSensorDataEndingBefore(time: CFAbsoluteTime) -> Void {
        let filtered = sensorDatas.filter { sd in
            let endTime = sd.endTime(self.header.sampleSize, samplesPerSecond: self.header.samplesPerSecond)
            return endTime >= time
        }
        var r: [SensorData] = []
        for sd in filtered {
            if let slice = sd.sliceFromStart(time, sampleSize: header.sampleSize, samplesPerSecond: header.samplesPerSecond) {
                r += [slice]
            }
        }
        
        sensorDatas = r
    }
    
    ///
    /// Computes slice of all sensor datas here into a continuous SDA, if possible
    ///
    func slice(range: TimeRange, maximumGap gap: CFTimeInterval, gapValue: UInt8) -> ContinuousSensorDataArray? {
        let samplesPerSecond = header.samplesPerSecond
        let sampleSize = header.sampleSize
        let bufferBytesCount = Int(floor(range.length * CFTimeInterval(samplesPerSecond) * CFTimeInterval(sampleSize)))
        if bufferBytesCount > 10 * 1024 * 1024 {
            // more than 10 MiB is almost certainly wrong
            fatalError("Request to allocate \(bufferBytesCount) B.")
        }
        
        var buffer = [UInt8](count: bufferBytesCount, repeatedValue: gapValue)
        
        let slicesInRange = sensorDatas.flatMap { (current: SensorData) -> (CFAbsoluteTime, [UInt8])? in
            if let slice = current.sliceSamples(range, sampleSize: sampleSize, samplesPerSecond: samplesPerSecond) {
                return (current.startTime, slice)
            }
            return nil
        }
        
        if slicesInRange.isEmpty { return nil }
        
        let slicesOrdered = slicesInRange.sorted { $0.0 <= $1.0 } // Order by start.
        for (start, bytes) in slicesOrdered {
            let bufferRangeStart = max (0, Int(floor((start - range.start) * CFTimeInterval(samplesPerSecond) * CFTimeInterval(sampleSize))))
            let bufferRangeCount = min(bufferBytesCount - bufferRangeStart, bytes.count)
            if bufferRangeCount > 0 {
                buffer[bufferRangeStart..<(bufferRangeStart + bufferRangeCount)] = bytes[0..<bufferRangeCount]
            }
        }
        
        let data = NSMutableData()
        data.appendBytes(buffer)
        return ContinuousSensorDataArray(header: header, sensorData: SensorData(startTime: range.start, samples: data))
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
class SensorData {
    /// the time the block of samples was received
    var startTime: CFAbsoluteTime
    /// the samples
    var samples: NSMutableData
    
    init(that: SensorData) {
        self.startTime = that.startTime
        self.samples = NSMutableData(data: that.samples)
    }
    
    init(startTime: CFAbsoluteTime, samples: NSData) {
        self.startTime = startTime
        self.samples = NSMutableData(data: samples)
    }
    
    init(startTime: CFAbsoluteTime, samples: NSMutableData) {
        self.startTime = startTime
        self.samples = samples
    }
    
    /// Append gap of given ``length`` filled with ``gapValue`` to the ``data``
    private func appendGap(length: Int, gapValue value: UInt8, toData data: NSMutableData) -> Void {
        let buf: [UInt8] = [UInt8](count: length, repeatedValue: value)
        data.appendBytes(buf, length: length)
    }
    
    ///
    /// Append another ``data`` to the ``samples``
    ///
    /* mutating */
    func append(samples data: NSData) {
        samples.appendData(data)
    }
    
    ///
    /// Removes ``count`` bytes from the end of the sample
    ///
    /* mutating */
    func removeFromEnd(count: Int) {
        if samples.length > count {
            samples.length -= count
        } else {
            samples.length = 0
        }
    }
    
    ///
    /// Pads end of this ``SensorData`` with ``gapValue`` until its end time matches ``until``.
    ///
    /* mutating */
    func padEnd(sampleSize: UInt8, samplesPerSecond: UInt8, gapValue: UInt8, until: CFTimeInterval) {
        let endTime = startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
        let endGap = until - endTime
        if endGap > 0 {
            let length = Int( endGap * Double(samplesPerSecond) * Double(sampleSize) )
            appendGap(length, gapValue: gapValue, toData: samples)
        }
    }
    
    ///
    /// Computes the end time of the sensor data block given the
    /// ``sampleSize`` and ``samplesPerSecond``
    ///
    func duration(sampleSize: UInt8, samplesPerSecond: UInt8) -> CFTimeInterval {
        let sampleCount = samples.length / Int(sampleSize)
        return CFTimeInterval(Double(sampleCount) / Double(samplesPerSecond))
    }
    
    ///
    /// Computes the end time of the sensor data block given the
    /// ``sampleSize`` and ``samplesPerSecond``
    ///
    func endTime(sampleSize: UInt8, samplesPerSecond: UInt8) -> CFAbsoluteTime {
        return startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
    }
    
    ///
    /// Slice from start to end
    ///
    func slice(#end: CFAbsoluteTime, maximumGap gap: CFTimeInterval, sampleSize: UInt8, samplesPerSecond: UInt8, gapValue: UInt8) -> SensorData? {
        return slice(start: startTime, trimmedTo: end, maximumGap: gap, sampleSize: sampleSize, samplesPerSecond: samplesPerSecond, gapValue: gapValue)
    }
    
    ///
    /// Computes slice of samples that fall within the given time range, 
    /// allowing for up to ``maximumGap`` before and after this data.
    /// Returns the ``SensorData`` with appropriately set ``startTime`` if
    /// the ``range`` is valid, or ``nil``.
    ///
    ///
    ///                                    startTime + duration |
    ///                                                         v
    /// [__######################################################]
    ///  ^ ^
    ///  | | startTime
    ///  |
    ///  | startTime - maximumGap
    ///
    func slice(#start: CFAbsoluteTime, trimmedTo end: CFAbsoluteTime, maximumGap gap: CFTimeInterval, sampleSize: UInt8, samplesPerSecond: UInt8, gapValue: UInt8) -> SensorData? {
        let endTime = startTime + duration(sampleSize, samplesPerSecond: samplesPerSecond)
        let startGap = startTime - start
        let trimTime = endTime > end ? end : endTime
        
        if startGap > gap { return nil }

        // start and length are in bytes: pointers to our ``sample``
        let start  = Int( -startGap * Double(samplesPerSecond) * Double(sampleSize) )
        let length = Int( (trimTime - startTime + startGap) * Double(samplesPerSecond) * Double(sampleSize) )

        let x = samples.length
        if start == 0 && length == x {
            // Exact match. 
            // Notice that we do the comparison here on Ints rather than the CFAbsoluteTimes above.
            return SensorData(that: self)
        } else if start >= 0 && (start + length) < samples.length {
            // Range is completely within our data
            return SensorData(startTime: startTime - startGap, samples: samples.subdataWithRange(NSMakeRange(start, length)))
        } else {
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
    
    ///
    /// Computes the sample bytes that fall into the time range indicated by ``range``, if any.
    ///
    func sliceSamples(range: TimeRange, sampleSize: UInt8, samplesPerSecond: UInt8) -> [UInt8]? {
        let thisRange = TimeRange(start: startTime, end: endTime(sampleSize, samplesPerSecond: samplesPerSecond))
        if !range.intersects(thisRange) { return nil }
        
        let startIndex = max(0, Int(ceil(((range.start - thisRange.start) * CFTimeInterval(samplesPerSecond) * CFTimeInterval(sampleSize)))))
        let count = min(samples.length - startIndex, Int(floor(range.length * CFTimeInterval(samplesPerSecond) * CFTimeInterval(sampleSize))))
        if count <= 0 { return nil }
        if count > 10 * 1024 * 1024 {
            fatalError("Request to slice \(count) B")
        }
        
        var buffer = [UInt8](count: count, repeatedValue: 0)
        samples.getBytes(&buffer, range: NSMakeRange(startIndex, count))
        return buffer
    }
    
    ///
    /// Creates a new ``SensorData`` that is computed by removing all samples that
    /// are recorded for the time before ``start``.
    ///
    func sliceFromStart(start: CFAbsoluteTime, sampleSize: UInt8, samplesPerSecond: UInt8) -> SensorData? {
        let thisRange = TimeRange(start: startTime, end: endTime(sampleSize, samplesPerSecond: samplesPerSecond))
        if thisRange.end <= start { return .None }
        if thisRange.start >= start { return self }
        if let bytes = sliceSamples(TimeRange(start: start, end: thisRange.end), sampleSize: sampleSize, samplesPerSecond: samplesPerSecond) {
            let data = NSMutableData()
            data.appendBytes(bytes)
            return SensorData(startTime: start, samples: data)
        }
        
        return .None
    }

}
