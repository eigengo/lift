import Foundation
import CoreMotion

class ThisDeviceSession : DeviceSession {
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue! = NSOperationQueue.currentQueue()
    private var sensorDataDelegate: SensorDataDelegate!
    private var count: Int = 0
    
    init(deviceInfo: DeviceInfo, sensorDataDelegate: SensorDataDelegate) {
        super.init(deviceInfo: deviceInfo)
        self.sensorDataDelegate = sensorDataDelegate
        motionManager = CMMotionManager()
        motionManager.deviceMotionUpdateInterval = NSTimeInterval(0.01)         // 10 ms ~> 100 Hz
        motionManager.startDeviceMotionUpdatesToQueue(queue, withHandler: processDeviceMotionData)
    }
    
    override func stop() {
        motionManager.stopDeviceMotionUpdates()
    }
    
    func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
        
        stats.update(DeviceSessionStatsTypes.Key(sensorKind: .Accelerometer, deviceId: ThisDevice.Info.id), update: { prev in
            return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 100, packets: prev.packets + 1)
        })
        stats.update(DeviceSessionStatsTypes.Key(sensorKind: .Gyroscope, deviceId: ThisDevice.Info.id), update: { prev in
            return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 100, packets: prev.packets + 1)
        })
        stats.update(DeviceSessionStatsTypes.Key(sensorKind: .GPS, deviceId: ThisDevice.Info.id), update: { prev in
            return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + 100, packets: prev.packets + 1)
        })
        
        count += 1
        
        if count % 100 == 0 {
            // TODO: Implement me
            sensorDataDelegate.sensorDataReceived(self, data: NSData())
        }
    }
    
}