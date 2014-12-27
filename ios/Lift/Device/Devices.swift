import Foundation

/** 
 * Holds all known devices (and possibly filters on not bought, etc...)
 */
struct Devices {
    internal static let devices : [Device] =
    [
        PebbleDevice(),
        AppleWatchDevice(),
        AndroidWearDevice()
    ]
    
    static func peek(onDone: DeviceInfo -> Void) {
        for d in devices { d.peek(onDone) }
    }
    
}