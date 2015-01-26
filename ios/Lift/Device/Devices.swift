import Foundation

/** 
 * Holds all known devices (and possibly filters on not bought, etc...)
 */
struct Devices {
    internal static let queue = dispatch_queue_create("devices", nil)
    internal static let devices : [Device] =
    [
        ThisDevice(),
        PebbleDevice(),
        AppleWatchDevice(),
        AndroidWearDevice(),
    ]
    
    static func peek(onDone: DeviceInfo -> Void) -> Void {
        for d in devices { d.peek(onDone) }
    }
    
    static func connectedDevices() -> [(Device, DeviceInfo)] {
        return allDevices().filter { $0.1.isConnected }
    }
    
    static func allDevices() -> [(Device, DeviceInfo)] {
        let done = NSCondition()
        var count: Int32 = Int32(devices.count)
        var result: [(Device, DeviceInfo)] = []
        dispatch_sync(queue, {
            for d in self.devices {
                d.peek { x in result += [(d, x)] }
            }
            if OSAtomicDecrement32(&count) == 0 {
                done.signal()
            }
        })
        done.wait()
        
        return result
    }
    
}