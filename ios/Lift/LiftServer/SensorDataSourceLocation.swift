import Foundation

/**
 * Sensor data location matching the Scala codebase
 */
enum SensorDataSourceLocation : UInt8 {
    case Wrist = 0x01
    case Waist = 0x02
    case Chest = 0x03
    case Foot  = 0x04
    case Any   = 0x7f
}