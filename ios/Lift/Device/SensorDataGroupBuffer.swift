import Foundation

let sensorDataGroupBufferQueue: dispatch_queue_t = dispatch_queue_create("SensorDataGroupBuffer", nil)

protocol SensorDataGroupBufferDelegate {
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedAt time: CFAbsoluteTime, data: NSData)
    
}

///
///
///
class SensorDataGroupBuffer {
    var sensorDataGroup: SensorDataGroup = SensorDataGroup()
    var lastDecodeTime: CFAbsoluteTime? = nil
    let windowSize: CFTimeInterval!
    let windowDelay: CFTimeInterval!
    let queue: dispatch_queue_t!
    let timer: dispatch_source_t!
    let delegate: SensorDataGroupBufferDelegate!
    let deviceLocations: DeviceId -> DeviceInfo.Location!
    var counter: UInt32 = 0
    
    init(delegate: SensorDataGroupBufferDelegate, queue: dispatch_queue_t, deviceLocations: DeviceId -> DeviceInfo.Location) {
        self.delegate = delegate
        self.deviceLocations = deviceLocations
        windowSize = Double(DevicePace.samplesPerPacket) / 100.0   // matches 124 samples at 100 Hz
        windowDelay = windowSize / 2.0
        timer = createDispatchTimer(windowSize, queue: queue, block: { self.encodeWindow() })
    }
    
    private func createDispatchTimer(interval: CFTimeInterval, queue: dispatch_queue_t, block: dispatch_block_t) -> dispatch_source_t {
        let timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, queue)
        if timer != nil  {
            let interval64: Int64 = Int64(interval * Double(NSEC_PER_SEC))
            dispatch_source_set_timer(timer, dispatch_time(DISPATCH_TIME_NOW, interval64), UInt64(interval64), NSEC_PER_SEC / 100)
            dispatch_source_set_event_handler(timer, block)
            dispatch_resume(timer)
        }
        return timer
    }

    /* mutating */
    func decodeAndAdd(data: NSData, fromDeviceId id: DeviceId, maximumGap gap: CFTimeInterval = 0.3, gapValue: UInt8 = 0x00) -> Void {
        let time = CFAbsoluteTimeGetCurrent()
        sensorDataGroup.decodeAndAdd(data, fromDeviceId: id, at: time, maximumGap: gap, gapValue: gapValue)
        
        lastDecodeTime = time
    }
    
    func stop() {
        dispatch_source_cancel(timer)
    }
    
    /* mutating */
    func encodeWindow() {
        NSLog("encodeWindow(): sensorDataGroup.rawCount = %d, sensorDataGroup.length = %d", sensorDataGroup.rawCount, sensorDataGroup.length)
        if let x = lastDecodeTime {
            let start = x - windowDelay - windowSize
            let end   = x - windowDelay
            
            let csdas = sensorDataGroup.continuousSensorDataArrays(within: TimeRange(start: start, end: end), maximumGap: 0.3, gapValue: 0x00)
            counter += 1
            if !csdas.isEmpty {
                if csdas.count > 255 { fatalError("Too many sensors") }
                let result = NSMutableData()
                result.appendUInt16(0xcab1)
                result.appendUInt8(UInt8(csdas.count))
                result.appendUInt32(counter)
                csdas.foreach { csda in
                    result.appendUInt16(UInt16(csda.length))
                    let location = self.deviceLocations(csda.header.sourceDeviceId)
                    result.appendUInt8(location.rawValue)
                    csda.encode(mutating: result)
                }
                
                delegate.sensorDataGroupBuffer(self, continuousSensorDataEncodedAt: start, data: result)
                NSLog("INFO: Data \(result.length)")
                sensorDataGroup.removeSensorDataArraysEndingBefore(start - windowSize)
            } else {
                NSLog("WARN: Empty range \(start) - \(end)")
            }
        }
        
    }
    
}