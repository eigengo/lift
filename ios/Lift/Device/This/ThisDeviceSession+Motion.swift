import Foundation
import CoreMotion

extension ThisDeviceSession {
    
    class Motion {
        private let motionManager: CMMotionManager!
        private let queue: NSOperationQueue! = NSOperationQueue.mainQueue()
        private let outer: ThisDeviceSession!
        private let samplesPerPacket = 40 //DevicePace.samplesPerPacket 

        private var count: Int = 0
        private var userAccelerationBuffer: NSMutableData!
        private var rotationBuffer: NSMutableData!
        #if arch(i386) || arch(x86_64)
        private var timer: dispatch_source_t!
        #endif

        init(outer: ThisDeviceSession) {
            self.outer = outer
            
            // CoreMotion initialization
        #if arch(i386) || arch(x86_64)
            timer = GCDTimer.createDispatchTimer(CFTimeInterval(0.01), queue: dispatch_get_main_queue(), block: { self.generateMotionData() })
        #else
            motionManager = CMMotionManager()
            motionManager.deviceMotionUpdateInterval = NSTimeInterval(0.01)         // 10 ms ~> 100 Hz
            motionManager.startDeviceMotionUpdatesToQueue(queue, withHandler: processDeviceMotionData)
        #endif
            userAccelerationBuffer = emptyAccelerationLikeBuffer(0xad)
            rotationBuffer = emptyAccelerationLikeBuffer(0xbd)
        }
        
        func stop() {
        #if arch(i386) || arch(x86_64)
            dispatch_source_cancel(timer)
        #else
            motionManager.stopDeviceMotionUpdates()
        #endif
        }
        
        #if arch(i386) || arch(x86_64)
        
        func generateMotionData() {
            processDeviceMotionData(DummyCMDeviceMotion(), error: nil)
        }
        
        #endif
        

        func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
            
            func appendRotation(rotation: CMRotationRate, toData data: NSMutableData) {
                // CMRotationRate units are in rad/s. We assume that the largest acceleration a user
                // will give to the device in the order of 10 rad/s; the units we send to the server
                // are therefore in 10 mrad/s
                
                var buffer = [UInt8](count: 5, repeatedValue: 0)
                let x = Int16(rotation.x * 100)  // crad/sec
                let y = Int16(rotation.y * 100)
                let z = Int16(rotation.z * 100)
                
                encode_lift_accelerometer_data(x, y, z, &buffer)
                data.appendBytes(&buffer, length: 5)
            }
            
            func appendAcceleration(acceleration: CMAcceleration, toData data: NSMutableData) {
                // CMAcceleration units are in 10 m/s^2. Pebble measures acceleration
                // in 10 mm/s^2, therefore, we multiply by 1000 to get the same unit.
                //
                // The acceleration vector is constructed so that it contains the gravity
                // vector too. See docs in CMDeviceMotion+MeasuredValue.swift.
                
                var buffer = [UInt8](count: 5, repeatedValue: 0)
                let x = Int16(acceleration.x * 1000)
                let y = Int16(acceleration.y * 1000)
                let z = Int16(acceleration.z * 1000)
                
                encode_lift_accelerometer_data(x, y, z, &buffer)
                data.appendBytes(&buffer, length: 5)
            }
            
            if count == samplesPerPacket {
                // Update our stats
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
            
            appendAcceleration(data.measuredAcceleration, toData: userAccelerationBuffer)
            appendRotation(data.rotationRate, toData: rotationBuffer)
            count += 1
        }
        
        private func emptyAccelerationLikeBuffer(type: UInt8) -> NSMutableData {
            let header: [UInt8] = [ type, UInt8(samplesPerPacket), 100, 5, 0 ]
            return NSMutableData(bytes: header, length: 5)
        }

    }
    
}

#if arch(i386) || arch(x86_64)
    class DummyCMDeviceMotion : CMDeviceMotion {
        private let _userAcceleration: CMAcceleration!
        private let _rotationRate: CMRotationRate!
        private let _gravity: CMAcceleration!
        
        override init() {
            _userAcceleration = CMAcceleration(x: 0, y: 0, z: 0)
            _gravity = CMAcceleration(x: 0, y: 0, z: 0)
            _rotationRate = CMRotationRate(x: 0, y: 0, z: 0)
            super.init()
        }
     
        required init(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        override var gravity: CMAcceleration {
            get {
                return _gravity
            }
        }
   
        override var userAcceleration: CMAcceleration {
            get {
                return _userAcceleration
            }
        }
        
        override var rotationRate: CMRotationRate {
            get {
                return _rotationRate
            }
        }
    }
    
#endif
