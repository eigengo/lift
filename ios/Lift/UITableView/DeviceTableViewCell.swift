import Foundation

/**
 * Implementation of a UITableViewCell for displaying information about a device.
 * 
 * TODO: Toggle optional edit / remove
 * TODO: Resolve image based on ``DeviceInfo.type``
 */
class DeviceTableViewCell : UITableViewCell {
    @IBOutlet var name: UILabel!
    @IBOutlet var detail: UILabel!
    
    func setDeviceInfo(deviceInfo: DeviceInfo?, deviceInfoDetail: DeviceInfo.Detail?) {
        if let di = deviceInfo {
            name.text = di.name
            if let did = deviceInfoDetail {
                detail.text = "DeviceTableViewCell.deviceInfoWithDetail".localized(di.serialNumber, did.address)
            } else {
                detail.text = "DeviceTableViewCell.deviceInfo".localized(di.serialNumber)
            }
        } else {
            name.text = "DeviceTableViewCell.noDevice".localized()
            detail.text = ""
        }
    }
    
    func setDeviceError(error: NSError) {
        name.text = String(format: "%@", error)
        detail.text = ""
    }
}
