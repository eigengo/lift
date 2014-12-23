import Foundation

/**
 * Holds the session stats for the accelerometer session
 */
class AccelerometerSessionStats {
    private var bytes: UInt = 0
    private var packets: UInt = 0
    private var start: NSTimeInterval?
    
    init() { }
    
    func receive(bytes: UInt16, packets: UInt16) {
        self.bytes += UInt(bytes)
        self.packets += UInt(packets)
        if start == nil {
            start = NSDate().timeIntervalSince1970
        }
    }
    
    /**
     * The total bytes received
     */
    var bytesReceived: UInt {
        get {
            return bytes
        }
    }
    
    /**
     * The total packets received
     */
    var packetsReceived: UInt {
        get {
            return packets;
        }
    }
    
    /**
     * Computes the bytes per second received by the device
     */
    func bytesPerSecond() -> Double? {
        if let s = start {
            let elapsed = NSDate().timeIntervalSince1970 - s
            return Double(bytes) / elapsed
        } else {
            return nil
        }
    }
}

/**
 * Implement to receive data and session identities
 */
protocol AccelerometerReceiverDelegate {
    /**
     * Called when accelerometer data (in the Lift format) is received from the device. The data is complete 
     * multiple of packets; it can be sent directly to the server for decoding.
     *
     * @param deviceSession the device session id
     * @param data the accelerometer data, aligned to packets
     * @param stats the session stats
     */
    func accelerometerReceiverReceived(deviceSession: NSUUID, data: NSData, stats: AccelerometerSessionStats)
    
    /**
     * Called when the device ends the session. Typically, a user presses a button on the device 
     * to stop the session.
     * 
     * @param deviceSession the device session id
     * @param stats the session stats
     */
    func accelerometerReceiverEnded(deviceSession: NSUUID, stats: AccelerometerSessionStats?)
}
