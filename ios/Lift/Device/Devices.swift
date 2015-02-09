import Foundation

/** 
 * Holds all known devices (and possibly filters on not bought, etc...)
 */
struct Devices {
    static let devices: [Device] = [
        ThisDevice(),
        PebbleDevice(),
        AppleWatchDevice(),
        AndroidWearDevice(),
    ]
    
    ///
    /// Peeks the devices, applyig ``onDone`` for each device that 
    /// responds with a ``DeviceInfo``
    ///
    static func peek(onDone: DeviceInfo -> Void) -> Void {
        for d in devices { d.peek(onDone) }
    }
    
}