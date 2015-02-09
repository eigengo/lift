import Foundation

let sensorDataGroupBufferQueue: dispatch_queue_t = dispatch_queue_create("SensorDataGroupBuffer", nil)

protocol SensorDataGroupBufferDelegate {
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedAt time: CFAbsoluteTime, data: NSData)
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, encodingSensorDataGroup group: SensorDataGroup)
    
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
        windowSize = 1.24
        windowDelay = 1.0
        timer = GCDTimer.createDispatchTimer(windowSize, queue: queue, block: { self.encodeWindow() })
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
        if let x = lastDecodeTime {
            delegate.sensorDataGroupBuffer(self, encodingSensorDataGroup: sensorDataGroup)
            
            let start = x - windowDelay
            let end   = x - windowDelay + windowSize
            
            if let range = sensorDataGroup.range {
                if range.start <= start && range.end >= end {
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
                        sensorDataGroup.removeSensorDataArraysEndingBefore(start)
                    }
                }
            }
        }
        
    }
    
}