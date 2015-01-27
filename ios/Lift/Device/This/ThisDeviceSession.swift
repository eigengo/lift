import Foundation
import CoreMotion

class ThisDeviceSession : DeviceSession {
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue! = NSOperationQueue.currentQueue()
    private var sensorDataDelegate: SensorDataDelegate!
    private var count: Int = 0
    
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
    
    func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
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