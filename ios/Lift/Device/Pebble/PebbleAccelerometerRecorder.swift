/*
import Foundation

class PebbleDataLoggingAccelerometer : NSObject, PBDataLoggingServiceDelegate {
    private var sessionStats: [UInt32 : AccelerometerSessionStats] = [:]
    private var incompletePackets: [UInt32 : NSData] = [:]
    private var delegate: AccelerometerDelegate
    
    required init(delegate: AccelerometerDelegate) {
        self.delegate = delegate
        super.init()

        PBPebbleCentral.defaultCentral().dataLoggingService.delegate = self
        PBPebbleCentral.defaultCentral().dataLoggingService.setDelegateQueue(nil)
    }
    
    private func sessionTagToDeviceSession(tag: UInt32) -> NSUUID {
        let uuid = String(format: "%00000000-0000-4000-a000-%012x", tag)
        return NSUUID(UUIDString: uuid)!
    }
    
    func dataLoggingService(service: PBDataLoggingService!, hasByteArrays bytes: UnsafePointer<UInt8>, numberOfItems: UInt16, forDataLoggingSession session: PBDataLoggingSessionMetadata!) -> Bool {
        objc_sync_enter(self)
        if sessionStats[session.tag] == nil {
            sessionStats[session.tag] = AccelerometerSessionStats()
        }
        let receivedData = NSMutableData(bytes: bytes, length: Int(numberOfItems))
        if let p = incompletePackets[session.tag] {
            
        }
        let stats = sessionStats[session.tag]!
        
        // TODO: check packet alignment
        
        stats.receive(numberOfItems, packets: 0)
        
        if receivedData.length > 0 {
            delegate.accelerometerReceiverReceived(sessionTagToDeviceSession(session.tag), data: receivedData, stats: stats)
        }
        
        objc_sync_exit(self)
        return true
    }
    
    func dataLoggingService(service: PBDataLoggingService!, sessionDidFinish session: PBDataLoggingSessionMetadata!) {
        delegate.accelerometerReceiverEnded(sessionTagToDeviceSession(session.tag), stats: sessionStats[session.tag])
    }
    
}
*/