import CoreMotion

/*
 * Motion recorder uses the accelerometer to record the data to a file for later analysis.
 */
class MotionRecorder {
    private var filePath: String
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue!
    private var buffer: NSString = ""
    var recording: Bool { get { return self.motionManager != nil } }
    
    init() {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true).first as String
        self.filePath = documentsPath.stringByAppendingPathComponent("accelerate.csv")
    }
    
    /*
     * Starts writing accelerometer events to the file. Calling this method more than once
     * has no effect.
     */
    func startRecording() {
        if self.motionManager == nil {
            self.motionManager = CMMotionManager()
            self.queue = NSOperationQueue.currentQueue() // NSOperationQueue();
            self.motionManager.startAccelerometerUpdatesToQueue(queue, withHandler: processAccelerometerData)
        }
    }
    
    /*
     * Stops writing accelerometer events to the file. Calling this method more than once has no further effect.
     */
    func stopRecording() {
        self.motionManager.stopAccelerometerUpdates()
        self.queue = nil
        self.motionManager = nil
    }
    
    func mark() {
        self.buffer = self.buffer + "\n"
        flush()
    }
    
    private func flush() {
        if self.buffer.length > 256 {
            let handle = NSFileHandle(forWritingAtPath: self.filePath)
            handle.seekToEndOfFile()
            handle.writeData(self.buffer.dataUsingEncoding(NSASCIIStringEncoding)!)
            handle.closeFile()
            self.buffer = ""
        }
    }
    
    func processAccelerometerData(data: CMAccelerometerData!, error: NSError!) -> Void {
        let row = NSString(format: "%@,%f,%f,%f\n", [NSDate(), data.acceleration.x, data.acceleration.y, data.acceleration.z])
        self.buffer = self.buffer + row
        flush()
    }
    
}