import CloudKit

/*
* Motion recorder uses the accelerometer to record the data to a file for later analysis.
*/
class PebbleAccelerometerRecorder : NSObject, PebbleAccelerometerReceiverDelegate {
    private let documentsPath: String = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true).first as String
    private var buffer: NSMutableData = NSMutableData()
    private var bytesReceived: UInt = 0
    private var bytesPerSecond: Double = 0

    private var filePath: String?
    private var start: NSTimeInterval?
    private var currentSession: PBDataLoggingSessionMetadata?
    
    private func createFileForSession(session: PBDataLoggingSessionMetadata!) {
        if currentSession != session {
            flush(true)
            currentSession = session
        }
        
        let fileName = NSString(format: "%@-%lf.dat", session.description, NSDate().timeIntervalSince1970)
        self.filePath = documentsPath.stringByAppendingPathComponent(fileName)
        if !NSFileManager.defaultManager().fileExistsAtPath(self.filePath!) {
            NSFileManager.defaultManager().createFileAtPath(self.filePath!, contents: nil, attributes: nil)
        }
    }
    
    func accelerometerReceiverReceived(data: NSData, bytesReceived: UInt, bytesPerSecond: Double, session: PBDataLoggingSessionMetadata!) {
        createFileForSession(session)
        buffer.appendData(data)
        flush(false)
    }
    
    func accelerometerReceiverEnded() {
        flush(true)
    }
    
    
    private func flush(force: Bool) {
        if self.filePath != nil && (self.buffer.length > 16384 || force) {
            let handle = NSFileHandle(forWritingAtPath: self.filePath!)!
            handle.seekToEndOfFile()
            handle.writeData(self.buffer)
            handle.closeFile()
            
            self.buffer.setData(NSData())
        }
    }
    
}