protocol PebbleAccelerometerReceiverDelegate {
    func accelerometerReceiverReceived(data: NSData, bytesReceived: UInt, bytesPerSecond: Double)
    func accelerometerReceiverEnded()
}

class PebbleAccelerometerReceiver : NSObject, PBDataLoggingServiceDelegate {
    private var bytesReceived: UInt = 0
    private var bytesPerSecond: Double = 0
    private var start: NSTimeInterval?
    var delegate: PebbleAccelerometerReceiverDelegate?
    
    override init() {
        super.init()

        PBPebbleCentral.defaultCentral().dataLoggingService.delegate = self
        PBPebbleCentral.defaultCentral().dataLoggingService.setDelegateQueue(nil)
    }
    
    func dataLoggingService(service: PBDataLoggingService!, hasByteArrays bytes: UnsafePointer<UInt8>, numberOfItems: UInt16, forDataLoggingSession session: PBDataLoggingSessionMetadata!) -> Bool {
        objc_sync_enter(self)
        bytesReceived = bytesReceived + UInt(numberOfItems)
        
        if numberOfItems > 0 && delegate != nil {
            delegate?.accelerometerReceiverReceived(NSData(bytes: bytes, length: Int(numberOfItems)), bytesReceived: bytesReceived, bytesPerSecond: bytesPerSecond)
        }
        
        if start != nil {
            let elapsed = NSDate().timeIntervalSince1970 - start!
            bytesPerSecond = Double(bytesReceived) / elapsed
        } else {
            start = NSDate().timeIntervalSince1970
        }
        
        objc_sync_exit(self)
        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        if delegate != nil {
            delegate?.accelerometerReceiverEnded()
        }
    }
    
}