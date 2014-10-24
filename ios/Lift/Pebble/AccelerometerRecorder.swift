import CloudKit

/*
* Motion recorder uses the accelerometer to record the data to a file for later analysis.
*/
class PebbleAccelerometerRecorder : NSObject, PebbleAccelerometerReceiverDelegate {
    private let documentsPath: String = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true).first as String
    
    private func writeData(data: NSData, session: UInt32) {
        let fileName = NSString(format: "%d-%lf.dat", session, NSDate().timeIntervalSince1970)
        let filePath = documentsPath.stringByAppendingPathComponent(fileName)
        if !NSFileManager.defaultManager().fileExistsAtPath(filePath) {
            NSFileManager.defaultManager().createFileAtPath(filePath, contents: nil, attributes: nil)
        }

        let handle = NSFileHandle(forWritingAtPath: filePath)!
        handle.seekToEndOfFile()
        handle.writeData(data)
        handle.closeFile()
    }
    
    func accelerometerReceiverReceived(data: NSData, session: UInt32, stats: AccelerometerSessionStats?) {
        writeData(data, session: session)
    }
    
    func accelerometerReceiverEnded(session: UInt32, stats: AccelerometerSessionStats?) {
    }
    
}