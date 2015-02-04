import Foundation

extension SensorData : Printable {
    var description: String {
        get {
            return NSString(format: "SensorData(startTime=%f, samples(length=%d)", self.startTime, self.samples.length)
        }
    }
}
