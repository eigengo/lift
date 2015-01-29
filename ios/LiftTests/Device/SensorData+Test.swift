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