import Foundation

class ThisDevice : NSObject, Device {
    private struct Info {
        static let id = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000001")!
        static let deviceInfo: DeviceInfo = {
            let serialNumber = UIDevice.currentDevice().identifierForVendor.UUIDString
            return DeviceInfo.ConnectedDeviceInfo(id: id, type: "this", name: UIDevice.currentDevice().name, serialNumber: serialNumber)
        }()
        static let deviceInfoDetail: DeviceInfo.Detail = {
            return DeviceInfo.Detail(address: "", hardwareVersion: UIDevice.currentDevice().model, osVersion: UIDevice.currentDevice().systemVersion)
        }()
    }
    
    // MARK: Device implementation
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(Info.deviceInfo)
    }
    
    func connect(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate, onDone: ConnectedDevice -> Void) {
        onDone(ThisConnectedDevice(deviceDelegate: deviceDelegate, sensorDataDelegate: sensorDataDelegate))
    }
    
}

class ThisConnectedDevice : ThisDevice, ConnectedDevice {
    var deviceDelegate: DeviceDelegate!
    var sensorDataDelegate: SensorDataDelegate!
    var currentDeviceSession: ThisDeviceSession?
    
    required init(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate) {
        self.deviceDelegate = deviceDelegate
        self.sensorDataDelegate = sensorDataDelegate
        super.init()
    }
    
    // MARK: ConnectedDevice implementation
    
    func start() {
        deviceDelegate.deviceGotDeviceInfo(Info.id, deviceInfo: Info.deviceInfo)
        deviceDelegate.deviceGotDeviceInfoDetail(Info.id, detail: Info.deviceInfoDetail)
        deviceDelegate.deviceAppLaunched(Info.id)
        currentDeviceSession?.stop()
        currentDeviceSession = ThisDeviceSession(deviceInfo: Info.deviceInfo)
    }
    
    func stop() {
        currentDeviceSession?.stop()
    }

}
