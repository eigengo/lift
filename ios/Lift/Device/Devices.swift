import Foundation

/** 
 * Holds all known devices (and possibly filters on not bought, etc...)
 */
struct Devices {
    internal static let devices : [Device] =
    [
        FitbitDevice(),
        PebbleDevice(),
        AppleWatchDevice(),
        AndroidWearDevice()
    ]
    
    static func peek(onDone: (Either<(NSError, DeviceType), DeviceInfo>) -> Void) {
        for d in devices { d.peek(onDone) }
    }
    
}