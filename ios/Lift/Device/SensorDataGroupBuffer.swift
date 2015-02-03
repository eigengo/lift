import Foundation

let sensorDataGroupBufferQueue: dispatch_queue_t = dispatch_queue_create("SensorDataGroupBuffer", nil)

protocol SensorDataGroupBufferDelegate {
    
    func sensorDataGroupBuffer(buffer: SensorDataGroupBuffer, continuousSensorDataEncodedAt time: CFAbsoluteTime, data: NSData)
    
}

struct SensorDataGroupBuffer {
    var sensorDataGroup: SensorDataGroup = SensorDataGroup()
    var lastDecodeTime: CFAbsoluteTime? = nil
    let windowSize: CFTimeInterval = Double(DevicePace.samplesPerPacket) / 100.0   // matches 124 samples at 100 Hz

    private func createDispatchTimer(interval: CFTimeInterval, queue: dispatch_queue_t, block: dispatch_block_t) -> dispatch_source_t {
        let timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, queue)
        if timer != nil  {
            let interval64: Int64 = Int64(interval * Double(NSEC_PER_SEC))
            dispatch_source_set_timer(timer, dispatch_time(DISPATCH_TIME_NOW, interval64), UInt64(interval64), (1 * NSEC_PER_SEC) / 10)
            dispatch_source_set_event_handler(timer, block)
            dispatch_resume(timer)
        }
        return timer
    }


    mutating func decodeAndAdd(data: NSData, fromDeviceId id: DeviceId, at time: CFAbsoluteTime, maximumGap gap: CFTimeInterval = 0.3, gapValue: UInt8 = 0x00) -> Void {
        sensorDataGroup.decodeAndAdd(data, fromDeviceId: id, at: time, maximumGap: gap, gapValue: gapValue)
    }
    
    func encode(range: TimeRange, maximumGap gap: CFTimeInterval, gapValue: UInt8) -> NSData? {
        let csdas = sensorDataGroup.continuousSensorDataArrays(within: range, maximumGap: gap, gapValue: gapValue)
        if csdas.isEmpty { return nil }
        
        let result = NSMutableData()
        csdas.foreach { $0.encode(mutating: result) }
        return result
    }
    
}