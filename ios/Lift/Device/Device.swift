import Foundation

struct DeviceInfo {
    var type: String
    var name: String
    var serialNumber: String
    var address: String
    
    var hardwareVersion: String
    var osVersion: String
}

protocol DeviceDelegate {
    
    func deviceDidNotConnect(error: NSError)
    
    func deviceAppLaunchFailed(deviceId: NSUUID, error: NSError)
    
    func deviceAppLaunched(deviceId: NSUUID, deviceInfo: DeviceInfo)
    
    func deviceDisconnected(deviceId: NSUUID)
    
}