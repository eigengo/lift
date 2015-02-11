import Foundation

let sensorDataGroupBufferQueue: dispatch_queue_t = dispatch_queue_create("SensorDataGroupBuffer", nil)

protocol SensorDataGroupBufferDelegate {
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedRange range: TimeRange, data: NSData)
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, encodingSensorDataGroup group: SensorDataGroup)
    
}

///
/// Collects, buffers and marks data for transfer.
///
class SensorDataGroupBuffer {
    var sensorDataGroup: SensorDataGroup = SensorDataGroup()
    let windowSize: CFTimeInterval!
    let windowDelay: CFTimeInterval!
    let encodeInterval: CFTimeInterval!
    let queue: dispatch_queue_t!
    let timer: dispatch_source_t!
    let delegate: SensorDataGroupBufferDelegate!
    let deviceLocations: DeviceId -> DeviceInfo.Location!
    var counter: UInt32 = 0
    
    init(delegate: SensorDataGroupBufferDelegate, queue: dispatch_queue_t, deviceLocations: DeviceId -> DeviceInfo.Location) {
        self.delegate = delegate
        self.deviceLocations = deviceLocations
        windowSize = 1.24
        windowDelay = 1.24
        encodeInterval = windowSize / 3
        timer = GCDTimer.createDispatchTimer(encodeInterval, queue: queue, block: { self.encodeWindow() })
    }
    
    /* mutating */
    func decodeAndAdd(data: NSData, fromDeviceId id: DeviceId, maximumGap gap: CFTimeInterval = 0.3, gapValue: UInt8 = 0x00) -> Void {
        let time = CFAbsoluteTimeGetCurrent()
        sensorDataGroup.decodeAndAdd(data, fromDeviceId: id, at: time, maximumGap: gap, gapValue: gapValue)
        delegate.sensorDataGroupBuffer(self, encodingSensorDataGroup: sensorDataGroup)
    }
    
    func stop() {
        dispatch_source_cancel(timer)
    }
    
    /* mutating */
    func encodeWindow() {
        delegate.sensorDataGroupBuffer(self, encodingSensorDataGroup: sensorDataGroup)
        
        if let range = sensorDataGroup.range {
            if range.length > windowSize + windowDelay {

                let start  = range.start
                let end    = range.start + windowSize
                if end + windowDelay >= CFAbsoluteTimeGetCurrent() { return }
                let window = TimeRange(start: start, end: end)
                
                let csdas = sensorDataGroup.continuousSensorDataArrays(within: window, maximumGap: window.length, gapValue: 0x00)
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
                    
                    delegate.sensorDataGroupBuffer(self, continuousSensorDataEncodedRange: window, data: result)
                    sensorDataGroup.removeSensorDataArraysEndingBefore(end)
                }
            }
        }
    }
    
}
