import Foundation

///
/// Implement to receive data and session identities
///
protocol DeviceSessionDelegate {

    ///
    /// The device session has started warming up, and the warmup is expected to complete in the given ``expectedCompletionIn``
    ///
    func deviceSession(session: DeviceSession, startedWarmingUp expectedCompletionIn: NSTimeInterval)
    
    ///
    /// The device session has finished warming up
    ///
    func deviceSession(session: DeviceSession, finishedWarmingUp: Void)
    
    ///
    /// Called when a sensor data (in the Lift format) is received from the device. The data is complete
    /// multiple of packets; it can be sent directly to the server for decoding.
    ///
    /// @param deviceSession the device session
    /// @param data the sensor data, aligned to packets
    ///
    func deviceSession(session: DeviceSession, sensorDataReceived data: NSData, fromDeviceId: DeviceId)
    
    ///
    /// Called when a sensor data is not received, but was expected. This typically indicates a
    /// problem with the BLE connection
    ///
    func deviceSession(session: DeviceSession, sensorDataNotReceived fromDeviceId: DeviceId)
    
    ///
    /// Called when the device ends the session. Typically, a user presses a button on the device
    /// to stop the session.
    ///
    /// @param deviceSession the device session
    ///
    func deviceSession(session: DeviceSession, ended fromDeviceId: DeviceId)
}
