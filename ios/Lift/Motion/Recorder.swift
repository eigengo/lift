import CoreMotion

/*
 * Motion recorder uses the accelerometer to record the data to a file for later analysis.
 */
class MotionRecorder {
    private var filePath: String
    private var motionManager: CMMotionManager!
    private var queue: NSOperationQueue!
    private var buffer: NSString = ""
    private var count: Int = 0
    private var callback: ((Int) -> Void)?
    var recording: Bool { get { return self.motionManager != nil } }

    init(name: String) {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true).first as String
        let fileName = NSString(format: "%@-%lf.csv", name, NSDate().timeIntervalSince1970)
        self.filePath = documentsPath.stringByAppendingPathComponent(fileName)
        if !NSFileManager.defaultManager().fileExistsAtPath(self.filePath) {
            NSFileManager.defaultManager().createFileAtPath(self.filePath, contents: nil, attributes: nil)
        }
    }
    
    /*
     * Starts writing accelerometer events to the file. Calling this method more than once
     * has no effect.
     */
    func startRecording(callback: ((Int) -> Void)?) {
        if self.motionManager == nil {
            self.motionManager = CMMotionManager()
            self.queue = NSOperationQueue.currentQueue() // NSOperationQueue();
            self.motionManager.startDeviceMotionUpdatesToQueue(self.queue, withHandler: processDeviceMotionData)
            self.motionManager.startAccelerometerUpdatesToQueue(self.queue, withHandler: processAccelerometerData)
            self.callback = callback
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
        
    private func flush() {
        if self.buffer.length > 256 {
            let handle = NSFileHandle(forWritingAtPath: self.filePath)
            handle.seekToEndOfFile()
            handle.writeData(self.buffer.dataUsingEncoding(NSASCIIStringEncoding)!)
            handle.closeFile()
            self.buffer = ""
        }
    }
    
    func processDeviceMotionData(data: CMDeviceMotion!, error: NSError!) -> Void {
        
    }

    func processAccelerometerData(data: CMAccelerometerData!, error: NSError!) -> Void {
        self.count++
        if (self.callback != nil) {
            self.callback!(self.count)
        }
        let row = NSString(format: "%f,%f,%f,%f\n", NSDate().timeIntervalSince1970, data.acceleration.x, data.acceleration.y, data.acceleration.z)
        self.buffer = self.buffer + row
        flush()
    }
    
}