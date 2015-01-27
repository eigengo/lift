import Foundation
import CoreMotion

///
/// Maintains a session connection to this device; measures acceleration, gyroscope, GPS,
/// and HR through HealthKit
///
class ThisDeviceSession : DeviceSession {
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue! = NSOperationQueue.currentQueue()
    private var sensorDataDelegate: SensorDataDelegate!
    private var count: Int = 0
    private var buffer: NSMutableData = NSMutableData()
    private let accelerationFactor: Double = 1000
    
    init(sensorDataDelegate: SensorDataDelegate) {
        super.init()
        self.sensorDataDelegate = sensorDataDelegate
        motionManager = CMMotionManager()
        motionManager.deviceMotionUpdateInterval = NSTimeInterval(0.01)         // 10 ms ~> 100 Hz
        motionManager.startDeviceMotionUpdatesToQueue(queue, withHandler: processDeviceMotionData)
    }
    
    override func stop() {
        motionManager.stopDeviceMotionUpdates()
    }
    
    func zero() -> Void {
        buffer = NSMutableData()
    }
    
    func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
        // TODO: userAcceleration units are in 10 m/s^2. Unfortunately, Pebble measures acceleration
        // in unitless numbers. Find conversion factor.
        
        // For now, I'll say that 4 G is the maximum force, and so our factor is 1000
        
        // I hate you Apple C chain. Y U no support ``struct { int16_t x_val : 13; }``?
        data.userAcceleration
        
        count += 1
        // TODO: Implement me
        
        if count % 100 == 0 {
            updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Accelerometer, deviceId: ThisDevice.Info.id), update: { prev in
                return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 625, packets: prev.packets + 1)
            })
            updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Gyroscope, deviceId: ThisDevice.Info.id), update: { prev in
                return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 625, packets: prev.packets + 1)
            })
            updateStats(DeviceSessionStatsTypes.Key(sensorKind: .GPS, deviceId: ThisDevice.Info.id), update: { prev in
                return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 625, packets: prev.packets + 1)
            })
        
            sensorDataDelegate.sensorDataReceived(ThisDevice.Info.id, deviceSession: self, data: NSData())
        }
    }
    
}