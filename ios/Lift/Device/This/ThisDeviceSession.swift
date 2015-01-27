import Foundation
import CoreMotion

///
/// Maintains a session connection to this device; measures acceleration, gyroscope, GPS,
/// and HR through HealthKit
///
class ThisDeviceSession : DeviceSession {
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue! = NSOperationQueue.currentQueue()
    private var deviceSessionDelegate: DeviceSessionDelegate!
    private var count: Int = 0
    private var userAccelerationBuffer: NSMutableData = NSMutableData()
    private let accelerationFactor: Double = 1000
    
    init(deviceSessionDelegate: DeviceSessionDelegate) {
        super.init()
        self.deviceSessionDelegate = deviceSessionDelegate
        motionManager = CMMotionManager()
        motionManager.deviceMotionUpdateInterval = NSTimeInterval(0.01)         // 10 ms ~> 100 Hz
        motionManager.startDeviceMotionUpdatesToQueue(queue, withHandler: processDeviceMotionData)
    }
    
    override func stop() {
        motionManager.stopDeviceMotionUpdates()
    }
    
    override func zero() -> NSTimeInterval {
        count = 0
        userAccelerationBuffer = NSMutableData()
        
        return 0    // we took no time to reset
    }
    
    func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
        
        func append(acceleration: CMAcceleration, toData data: NSMutableData) {
            // TODO: userAcceleration units are in 10 m/s^2. Unfortunately, Pebble measures acceleration
            // in unitless numbers. Find conversion factor.
            // For now, I'll say that 4 G is the maximum force, and so our factor is 1000

            var buffer = [UInt8](count: 5, repeatedValue: 0)
            let x = Int32(acceleration.x * 1000)
            let y = Int32(acceleration.y * 1000)
            let z = Int32(acceleration.z * 1000)
            
            encode_lift_accelerometer_data(x, y, z, &buffer)
            data.appendBytes(&buffer, length: 5)
        }
        
        count += 1
        
        append(data.userAcceleration, toData: userAccelerationBuffer)
        
        if count == DevicePace.samplesPerPacket {
            // We have collected enough data to make up a packet.
            // Combine all buffers and send to the delegate
            let data = NSMutableData(data: userAccelerationBuffer)
            deviceSessionDelegate.deviceSession(self, sensorDataReceived: data, fromDeviceId: ThisDevice.Info.id)

            // Clear buffers
            zero()
            
            // Update our stats
            updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Accelerometer, deviceId: ThisDevice.Info.id), update: { prev in
                return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 625, packets: prev.packets + 1)
            })
            updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Gyroscope, deviceId: ThisDevice.Info.id), update: { prev in
                return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 625, packets: prev.packets + 1)
            })
            updateStats(DeviceSessionStatsTypes.Key(sensorKind: .GPS, deviceId: ThisDevice.Info.id), update: { prev in
                return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 625, packets: prev.packets + 1)
            })
        }
        
    }
    
}