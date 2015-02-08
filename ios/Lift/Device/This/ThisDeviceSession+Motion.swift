import Foundation
import CoreMotion

extension ThisDeviceSession {
    
    class Motion {
        private let motionManager: CMMotionManager!
        private let queue: NSOperationQueue! = NSOperationQueue.mainQueue()
        private let outer: ThisDeviceSession!

        private var count: Int = 0
        private var userAccelerationBuffer: NSMutableData!
        private var rotationBuffer: NSMutableData!

        init(outer: ThisDeviceSession) {
            self.outer = outer
            
            // CoreMotion initialization
            motionManager = CMMotionManager()
            motionManager.deviceMotionUpdateInterval = NSTimeInterval(0.01)         // 10 ms ~> 100 Hz
            motionManager.startDeviceMotionUpdatesToQueue(queue, withHandler: processDeviceMotionData)
            userAccelerationBuffer = emptyAccelerationLikeBuffer(0xad)
            rotationBuffer = emptyAccelerationLikeBuffer(0xbd)
        }
        
        func stop() {
            motionManager.stopDeviceMotionUpdates()
        }

        func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
            
            func appendRotation(rotation: CMRotationRate, toData data: NSMutableData) {
                // CMRotationRate units are in rad/s. We assume that the largest acceleration a user
                // will give to the device in the order of 10 rad/s; the units we send to the server
                // are therefore in mrad/s
                
                var buffer = [UInt8](count: 5, repeatedValue: 0)
                let x = Int16(rotation.x * 100)  // mrad/sec
                let y = Int16(rotation.y * 100)
                let z = Int16(rotation.z * 100)
                
                encode_lift_accelerometer_data(x, y, z, &buffer)
                data.appendBytes(&buffer, length: 5)
            }
            
            func appendAcceleration(acceleration: CMAcceleration, toData data: NSMutableData) {
                // CMAcceleration units are in 10 m/s^2. Pebble measures acceleration
                // in 10 mm/s^2, therefore, we multiply by 1000 to get the same unit.
                
                var buffer = [UInt8](count: 5, repeatedValue: 0)
                let x = Int16(acceleration.x * 1000)
                let y = Int16(acceleration.y * 1000)
                let z = Int16(acceleration.z * 1000)
                
                encode_lift_accelerometer_data(x, y, z, &buffer)
                data.appendBytes(&buffer, length: 5)
            }
            
            if count == DevicePace.samplesPerPacket {
                // Update our stats
                NSLog("Buffer with \(userAccelerationBuffer.length) with count \(count)")
                outer.updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Accelerometer, deviceId: ThisDevice.Info.id), update: { prev in
                    return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + self.userAccelerationBuffer.length, packets: prev.packets + 1)
                })
                outer.updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Gyroscope, deviceId: ThisDevice.Info.id), update: { prev in
                    return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + self.rotationBuffer.length, packets: prev.packets + 1)
                })
                
                // We have collected enough data to make up a packet.
                // Combine all buffers and send to the delegate
                let data = NSMutableData(data: userAccelerationBuffer)
                data.appendData(rotationBuffer)
                outer.submitDeviceData(data)
                
                // Clear buffers
                count = 0
                userAccelerationBuffer = emptyAccelerationLikeBuffer(0xad)
                rotationBuffer = emptyAccelerationLikeBuffer(0xbd)
            }
            
            appendAcceleration(data.userAcceleration, toData: userAccelerationBuffer)
            appendRotation(data.rotationRate, toData: rotationBuffer)
            count += 1
        }
        
        private func emptyAccelerationLikeBuffer(type: UInt8) -> NSMutableData {
            let header: [UInt8] = [ type, 124, 100, 5, 0 ]
            return NSMutableData(bytes: header, length: 5)
        }

    }
    
}