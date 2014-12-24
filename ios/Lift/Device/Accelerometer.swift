import Foundation

/**
 * Implement to receive data and session identities
 */
protocol AccelerometerDelegate {
    /**
     * Called when accelerometer data (in the Lift format) is received from the device. The data is complete 
     * multiple of packets; it can be sent directly to the server for decoding.
     *
     * @param deviceSession the device session
     * @param data the accelerometer data, aligned to packets
     */
    func accelerometerDataReceived(deviceSession: DeviceSession, data: NSData)
    
    /**
     * Called when the device ends the session. Typically, a user presses a button on the device 
     * to stop the session.
     * 
     * @param deviceSession the device session
     */
    func accelerometerDataEnded(deviceSession: DeviceSession)
}
