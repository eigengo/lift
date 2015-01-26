import Foundation

class MultiDeviceSensorDataDelegate : SensorDataDelegate {
    var stats: DeviceSessionStats = DeviceSessionStats()
    var delegate: SensorDataDelegate!
    var devices: [DeviceInfo]!
    var updateInterval: NSTimeInterval
    
    init(devices: [DeviceInfo], updateInterval: NSTimeInterval, delegate: SensorDataDelegate) {
        self.delegate = delegate
        self.devices = devices
        self.updateInterval = updateInterval
    }
    
    func sensorDataEnded(deviceSession: DeviceSession) {
        // ???
    }
    
    func sensorDataReceived(deviceSession: DeviceSession, data: NSData) {
        // ???
    }
}