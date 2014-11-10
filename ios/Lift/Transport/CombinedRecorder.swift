import Foundation

class CombinedAccelerometerRecorder : NSObject, AccelerometerReceiverDelegate {
    private var recorders: [AccelerometerReceiverDelegate]
    
    init(recorders: [AccelerometerReceiverDelegate]) {
        self.recorders = recorders;
        super.init()
    }
    
    func accelerometerReceiverReceived(data: NSData, session: UInt32, stats: AccelerometerSessionStats?) {
        for recorder in recorders {
            recorder.accelerometerReceiverReceived(data, session: session, stats: stats)
        }
    }
    
    func accelerometerReceiverEnded(session: UInt32, stats: AccelerometerSessionStats?) {
        for recorder in recorders {
            recorder.accelerometerReceiverEnded(session, stats: stats)
        }
    }
    
}