import Foundation
import CoreMotion

class ThisDeviceSession : DeviceSession {
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue! = NSOperationQueue.currentQueue()
    
    override init(deviceInfo: DeviceInfo) {
        super.init(deviceInfo: deviceInfo)
        motionManager = CMMotionManager()
        motionManager.deviceMotionUpdateInterval = NSTimeInterval(0.01)         // 10 ms ~> 100 Hz
        motionManager.startDeviceMotionUpdatesToQueue(queue, withHandler: processDeviceMotionData)
    }
    
    override func stop() {
        motionManager.stopDeviceMotionUpdates()
    }
    
    func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
        NSLog(".")
    }
    
}