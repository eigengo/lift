import Foundation

internal struct DeviceTableViewCellImages {
    static let images: [DeviceType : UIImage] =
        [
            "pebble"      : UIImage(named: "pebble")!,
            "androidwear" : UIImage(named: "androidwear")!,
            "applewatch"  : UIImage(named: "applewatch")!,
            "this"        : UIImage(named: "this")!
    ]
}

///
/// Implementation of a UITableViewCell for displaying information about a device.
///
class DeviceTableViewCell : UITableViewCell {
    @IBOutlet var name: UILabel!
    @IBOutlet var detail: UILabel!
    @IBOutlet var typeImage: UIImageView!
    private var deviceId: DeviceId?
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        selectionStyle = UITableViewCellSelectionStyle.None
    }
    
    private func setDeviceInfo(deviceInfo: DeviceInfo, deviceInfoDetail: DeviceInfo.Detail?) {
        typeImage.image = DeviceTableViewCellImages.images[deviceInfo.type]
        switch deviceInfo {
        case .ConnectedDeviceInfo(let id, let t, let n, let d):
            name.text = n
            name.textColor = tintColor
            detail.textColor = UIColor.blackColor()
            deviceId = id
            if let did = deviceInfoDetail {
                detail.text = "DeviceTableViewCell.deviceInfoWithDetail".localized(d, did.address)
            } else {
                detail.text = "DeviceTableViewCell.deviceInfo".localized(d)
            }
        case .DisconnectedDeviceInfo(let id, let t, let e):
            typeImage.alpha = 0.4
            name.textColor = UIColor.grayColor()
            detail.textColor = UIColor.grayColor()
            deviceId = id
            name.text = ("DeviceTableViewCell.disconnectedType." + t).localized()
            let description = e != nil ? e!.localizedDescription : ""
            detail.text = "DeviceTableViewCell.disconnected".localized(description)
        case .NotAvailableDeviceInfo(let t, let e):
            typeImage.alpha = 0.4
            name.textColor = UIColor.grayColor()
            detail.textColor = UIColor.grayColor()
            name.text = ("DeviceTableViewCell.notAvailableType." + t).localized()
            detail.text = "DeviceTableViewCell.notAvailable".localized(e.localizedDescription)
        }
    }
    
    func setDeviceInfosAndDelegate(deviceInfo: DeviceInfo?, deviceInfoDetail: DeviceInfo.Detail?) {
        if let di = deviceInfo {
            setDeviceInfo(di, deviceInfoDetail: deviceInfoDetail)
        } else {
            name.text = "DeviceTableViewCell.noDevice".localized()
            detail.text = ""
        }
    }
}
