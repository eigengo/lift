import Foundation

class ThisDevice : NSObject, Device {
    internal struct Info {
        static let id = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000001")!
        static let deviceInfo: DeviceInfo = {
            return DeviceInfo.ConnectedDeviceInfo(id: id, type: "this", name: UIDevice.currentDevice().name, description: UIDevice.currentDevice().localizedModel)
        }()
        static let deviceInfoDetail: DeviceInfo.Detail = {
            return DeviceInfo.Detail(address: "(This device)".localized(), hardwareVersion: UIDevice.currentDevice().model, osVersion: UIDevice.currentDevice().systemVersion)
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
        currentDeviceSession = ThisDeviceSession(sensorDataDelegate: sensorDataDelegate)
    }
    
    func stop() {
        currentDeviceSession?.stop()
    }
    
    func zero() -> NSTimeInterval {
        if let x = currentDeviceSession {
            x.zero()
        }
        return 0
    }

}
