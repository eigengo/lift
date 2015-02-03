import Foundation

///
/// Implement to receive data and session identities
///
protocol DeviceSessionDelegate {
    
    ///
    /// Called when a sensor data (in the Lift format) is received from the device. The data is complete
    /// multiple of packets; it can be sent directly to the server for decoding.
    ///
    /// @param session the device session
    /// @param deviceId the device from which the data was received
    /// @time the device time
    /// @param data the sensor data, aligned to packets
    ///
    func deviceSession(session: DeviceSession, sensorDataReceivedFrom deviceId: DeviceId, atDeviceTime time: CFAbsoluteTime, data: NSData)
    
    ///
    /// Called when a sensor data is not received, but was expected. This typically indicates a
    /// problem with the BLE connection
    ///
    func deviceSession(session: DeviceSession, sensorDataNotReceivedFrom deviceId: DeviceId)
    
    ///
    /// Called when the device ends the session. Typically, a user presses a button on the device
    /// to stop the session.
    ///
    /// @param deviceSession the device session
    ///
    func deviceSession(session: DeviceSession, endedFrom deviceId: DeviceId)
}
