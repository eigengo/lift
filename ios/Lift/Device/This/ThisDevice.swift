import Foundation

class ThisDevice : NSObject, Device {
    let thisDeviceUUID = NSUUID(UUIDString: "00000000-0000-0000-0000-000000000001")!
    
    func peek(onDone: DeviceInfo -> Void) {
        let serialNumber = UIDevice.currentDevice().identifierForVendor.UUIDString
        let info = DeviceInfo.ConnectedDeviceInfo(id: thisDeviceUUID, type: UIDevice.currentDevice().model, name: UIDevice.currentDevice().name, serialNumber: serialNumber)
        onDone(info)
    }
    
}