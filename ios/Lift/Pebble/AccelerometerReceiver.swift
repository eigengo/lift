class AccelerometerSessionStats {
    private var _bytesReceived: UInt = 0
    private var _bytesPerSecond: Double = 0
    private var start: NSTimeInterval?
    
    init() { }
    
    func receive(numberOfItems: UInt16) {
        self._bytesReceived += UInt(numberOfItems)
        if start != nil {
            let elapsed = NSDate().timeIntervalSince1970 - start!
            _bytesPerSecond = Double(_bytesReceived) / elapsed
        } else {
            start = NSDate().timeIntervalSince1970
        }
    }
    
    var bytesReceived: UInt {
        get {
            return _bytesReceived
        }
    }
    
    var bytesPerSecond: Double {
        get {
            return _bytesPerSecond
        }
    }
}

protocol PebbleAccelerometerReceiverDelegate {
    func accelerometerReceiverReceived(data: NSData, session: UInt32, stats: AccelerometerSessionStats?)
    func accelerometerReceiverEnded(session: UInt32, stats: AccelerometerSessionStats?)
}

class PebbleAccelerometerReceiver : NSObject, PBDataLoggingServiceDelegate {
    private var sessionStats: [UInt32: AccelerometerSessionStats] = [:]
    var delegate: PebbleAccelerometerReceiverDelegate?
    
    override init() {
        super.init()

        PBPebbleCentral.defaultCentral().dataLoggingService.delegate = self
        PBPebbleCentral.defaultCentral().dataLoggingService.setDelegateQueue(nil)
    }
    
    func dataLoggingService(service: PBDataLoggingService!, hasByteArrays bytes: UnsafePointer<UInt8>, numberOfItems: UInt16, forDataLoggingSession session: PBDataLoggingSessionMetadata!) -> Bool {
        objc_sync_enter(self)
        if sessionStats[session.tag] == nil {
            sessionStats[session.tag] = AccelerometerSessionStats()
        }
        let stats = sessionStats[session.tag]!
        stats.receive(numberOfItems)
        
        if numberOfItems > 0 && delegate != nil {
            delegate?.accelerometerReceiverReceived(NSData(bytes: bytes, length: Int(numberOfItems)), session: session.tag, stats: stats)
        }

        objc_sync_exit(self)
        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        if delegate != nil {
            delegate?.accelerometerReceiverEnded(session.tag, stats: sessionStats[session.tag])
        }
    }
    
}