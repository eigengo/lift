import Foundation

///
/// Grand Central Dispatch timer
///
struct GCDTimer {
    
    static func createDispatchTimer(interval: CFTimeInterval, queue: dispatch_queue_t, block: dispatch_block_t) -> dispatch_source_t {
        let timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, queue)
        if timer != nil  {
            let interval64: Int64 = Int64(interval * Double(NSEC_PER_SEC))
            dispatch_source_set_timer(timer, dispatch_time(DISPATCH_TIME_NOW, interval64), UInt64(interval64), NSEC_PER_SEC / 100)
            dispatch_source_set_event_handler(timer, block)
            dispatch_resume(timer)
        }
        return timer
    }

}
