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
 * Implementations must provide ways to access the accessory switch
 */
protocol DeviceTableViewCellDelegate {
    
    /**
     * Show the switch for given device id?
     * 
     * @param deviceId the device identity
     * @return true if the switch should appear
     */
    func deviceTableViewCellShowAccessorySwitch(deviceId: DeviceId) -> Bool
    
    /**
     * Obtain value for the switch for the given device id
     * 
     * @param deviceId the device identity
     * @return the value for the switch
     */
    func deviceTableViewCellAccessorySwitchValue(deviceId: DeviceId) -> Bool
    
    /**
     * Called when the switche's value changes. The implementation class has the opportunity
     * to "reject" any changes by returning an override value that the switch will take on.
     *
     * @param deviceId the device identity
     * @param value the new value
     * @return the value that the caller determines should now be displayed by the switch
     */
    func deviceTableViewCellAccessorySwitchValueChanged(deviceId: DeviceId, value: Bool) -> Bool
    
}

/**
 * Implementation of a UITableViewCell for displaying information about a device.
 */
class DeviceTableViewCell : UITableViewCell {
    @IBOutlet var name: UILabel!
    @IBOutlet var detail: UILabel!
    @IBOutlet var typeImage: UIImageView!
    @IBOutlet var accessorySwitch: UISwitch!
    private var delegate: DeviceTableViewCellDelegate?
    private var deviceId: DeviceId?
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        selectionStyle = UITableViewCellSelectionStyle.None
    }
    
    private func setDeviceInfo(deviceInfo: DeviceInfo, deviceInfoDetail: DeviceInfo.Detail?) {
        typeImage.image = DeviceTableViewCellImages.images[deviceInfo.type]
        switch deviceInfo {
        case .ConnectedDeviceInfo(let id, let t, let n, let sn):
            name.text = n
            name.textColor = tintColor
            detail.textColor = UIColor.blackColor()
            deviceId = id
            if let did = deviceInfoDetail {
                detail.text = "DeviceTableViewCell.deviceInfoWithDetail".localized(sn, did.address)
            } else {
                detail.text = "DeviceTableViewCell.deviceInfo".localized(sn)
            }
            if let d = delegate {
                accessorySwitch.on = d.deviceTableViewCellAccessorySwitchValue(id)
                accessorySwitch.hidden = !d.deviceTableViewCellShowAccessorySwitch(id)
            } else {
                accessorySwitch.hidden = true
            }
        case .DisconnectedDeviceInfo(let id, let t, let e):
            typeImage.alpha = 0.4
            name.textColor = UIColor.grayColor()
            detail.textColor = UIColor.grayColor()
            deviceId = id
            name.text = ("DeviceTableViewCell.disconnectedType." + t).localized()
            let description = e != nil ? e!.localizedDescription : ""
            detail.text = "DeviceTableViewCell.disconnected".localized(description)
            accessorySwitch.hidden = true
            if let d = delegate {
                accessorySwitch.on = d.deviceTableViewCellAccessorySwitchValue(id)
                accessorySwitch.hidden = !d.deviceTableViewCellShowAccessorySwitch(id)
            } else {
                accessorySwitch.hidden = true
            }
        case .NotAvailableDeviceInfo(let t, let e):
            typeImage.alpha = 0.4
            name.textColor = UIColor.grayColor()
            detail.textColor = UIColor.grayColor()
            name.text = ("DeviceTableViewCell.notAvailableType." + t).localized()
            detail.text = "DeviceTableViewCell.notAvailable".localized(e.localizedDescription)
            accessorySwitch.hidden = true
        }
    }
    
    func setDeviceInfosAndDelegate(deviceInfo: DeviceInfo?, deviceInfoDetail: DeviceInfo.Detail?, delegate: DeviceTableViewCellDelegate?) {
        self.delegate = delegate
        if let di = deviceInfo {
            setDeviceInfo(di, deviceInfoDetail: deviceInfoDetail)
        } else {
            accessorySwitch.hidden = true
            name.text = "DeviceTableViewCell.noDevice".localized()
            detail.text = ""
        }
    }
    
    @IBAction
    func accessorySwitchChanged() {
        if let x = deviceId {
            if let d = delegate {
                accessorySwitch.on = d.deviceTableViewCellAccessorySwitchValueChanged(x, value: accessorySwitch.on)
            }
        }
    }

}
