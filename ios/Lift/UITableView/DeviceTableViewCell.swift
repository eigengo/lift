import Foundation

internal struct DeviceTableViewCellImages {
    static let images: [DeviceType : UIImage] =
        [
            "pebble"      : UIImage(named: "pebble")!,
            "androidwear" : UIImage(named: "androidwear")!,
            "applewatch"  : UIImage(named: "applewatch")!
    ]
}

/**
 * Implementation of a UITableViewCell for displaying information about a device.
 * 
 * TODO: Toggle optional edit / remove
 */
class DeviceTableViewCell : UITableViewCell {
    @IBOutlet var name: UILabel!
    @IBOutlet var detail: UILabel!
    @IBOutlet var typeImage: UIImageView!
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        selectionStyle = UITableViewCellSelectionStyle.None
    }
    
    private func setDeviceInfo(deviceInfo: DeviceInfo, deviceInfoDetail: DeviceInfo.Detail?) {
        typeImage.image = DeviceTableViewCellImages.images[deviceInfo.type]
        switch deviceInfo {
        case .ConnectedDeviceInfo(_, let t, let n, let sn):
            name.text = n
            name.textColor = tintColor
            if let did = deviceInfoDetail {
                detail.text = "DeviceTableViewCell.deviceInfoWithDetail".localized(sn, did.address)
            } else {
                detail.text = "DeviceTableViewCell.deviceInfo".localized(sn)
            }
        case .DisconnectedDeviceInfo(_, let t, let e):
            typeImage.alpha = 0.4
            name.textColor = UIColor.grayColor()
            name.text = ("DeviceTableViewCell.disconnectedType." + t).localized()
            let description = e != nil ? e!.localizedDescription : ""
            detail.text = "DeviceTableViewCell.disconnected".localized(description)
        case .NotAvailableDeviceInfo(let t, let e):
            typeImage.alpha = 0.4
            name.textColor = UIColor.grayColor()
            name.text = ("DeviceTableViewCell.notAvailableType." + t).localized()
            detail.text = "DeviceTableViewCell.notAvailable".localized(e.localizedDescription)
            
        }
    }
    
    func setDeviceInfo(deviceInfo: DeviceInfo?, deviceInfoDetail: DeviceInfo.Detail?) {
        if let di = deviceInfo {
            setDeviceInfo(di, deviceInfoDetail: deviceInfoDetail)
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
