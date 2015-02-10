import Foundation

extension SensorDataGroup {
    
    ///
    /// Returns the start time of the earliest block of data
    ///
    var startTime: CFAbsoluteTime? {
        get {
            return sensorDataArrays.flatMap { $0.startTime }.minBy(identity)
        }
    }
    
    ///
    /// Returns the end time of the last block of data
    ///
    var endTime: CFAbsoluteTime? {
        get {
            return sensorDataArrays.flatMap { $0.endTime }.maxBy(identity)
        }
    }
    
    ///
    /// Returns the time range of the data group
    ///
    var range: TimeRange? {
        get {
            if let x = startTime {
                return TimeRange(start: x, end: endTime!)
            }
            return nil
        }
    }
    
}


extension SensorDataArray {
    
    ///
    /// Returns the start time of the earliest block of data
    ///
    var startTime: CFAbsoluteTime? {
        get {
            return sensorDatas.map { $0.startTime }.minBy(identity)
        }
    }
    
    ///
    /// Returns the end time of the last block of data
    ///
    var endTime: CFAbsoluteTime? {
        get {
            return sensorDatas.map { $0.endTime(self.header.sampleSize, samplesPerSecond: self.header.samplesPerSecond) }.maxBy(identity)
        }
    }
    
    var longestAvailablePeriod: CFTimeInterval? {
        get {
            if let start = startTime {
                if let end = endTime {
                    return end - start
                }
            }
            
            return .None
        }
    }
    
}
