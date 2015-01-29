import Foundation

///
/// Convenience test extension to SensorData to allow us to manipulate it using string payloads
///
extension SensorData {
    
    ///
    /// Construct SensorData from the given string ``s`` at the starting ``time``
    ///
    static func fromString(s: String, startingAt time: CFAbsoluteTime) -> SensorData {
        return SensorData(startTime: time, samples: s.dataUsingEncoding(NSASCIIStringEncoding, allowLossyConversion: false)!)
    }
    
    ///
    /// Interpret the ``samples`` as ``String``
    ///
    func asString() -> String {
        return NSString(data: samples, encoding: NSASCIIStringEncoding)!
    }
    
}

///
/// Equatable implementation for SensorDataArray
///
extension SensorDataArray : Equatable {
    
}

func ==(lhs: SensorDataArray, rhs: SensorDataArray) -> Bool {
    if lhs.header == rhs.header {
        if lhs.sensorDatas.count != rhs.sensorDatas.count { return false }
        for (i, lsd) in enumerate(lhs.sensorDatas) {
            let rsd = rhs.sensorDatas[i]
            if lsd != rsd { return false }
        }
        return true
    }
    return false
}

///
/// Equatable implementation for SensorData
///
extension SensorData : Equatable {
    
}

func ==(lhs: SensorData, rhs: SensorData) -> Bool {
    return lhs.startTime =~= rhs.startTime && lhs.samples.isEqualToData(rhs.samples)
}