import Foundation
import CoreMotion
import HealthKit

///
/// Maintains a session connection to this device; measures acceleration, gyroscope, GPS,
/// and HR through HealthKit
///
class ThisDeviceSession : DeviceSession {
    private var deviceSessionDelegate: DeviceSessionDelegate!

    private let motion: Motion!
    private let healthKit: HealthKit!
    
    init(deviceSessionDelegate: DeviceSessionDelegate) {
        super.init()
        self.deviceSessionDelegate = deviceSessionDelegate
        
        // CoreMotion initialization
        motion = Motion(outer: self)
        
        // HealthKit initialization
        healthKit = HealthKit()
    }
    
    override func stop() {
        healthKit.stop()
        motion.stop()
    }
    
    override func zero() -> NSTimeInterval {
        return 0    // we took no time to reset
    }
    
    func submitDeviceData(data: NSData) {
        deviceSessionDelegate.deviceSession(self, sensorDataReceivedFrom: ThisDevice.Info.id, atDeviceTime: CFAbsoluteTimeGetCurrent(), data: data)
    }
    
}