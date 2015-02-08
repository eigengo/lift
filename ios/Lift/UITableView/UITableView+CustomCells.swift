import Foundation

struct UITableViewCellReuseIdentifiers {
    internal static let device = "__device"
    internal static let property = "__property"
}

///
/// Convenience extension to UITableViewController to allow the custom cells to be dequeued
/// by loading them from their matching nibs
///
extension UITableView {
    
    private func doDequeue<A where A : UITableViewCell>(reuseIdentifier: String, register: UITableView -> Void) -> A {
        let cell = self.dequeueReusableCellWithIdentifier(reuseIdentifier) as A?
        
        if let x = cell { return x } else {
            register(self)
            return self.dequeueReusableCellWithIdentifier(reuseIdentifier) as A
        }
    }
    
    func dequeueReusablePropertyTableViewCell(property: String, delegate: PropertyTableViewCellDelegate) -> PropertyTableViewCell {
        let cell: PropertyTableViewCell = doDequeue(UITableViewCellReuseIdentifiers.property) {
            $0.registerNib(UINib(nibName: "PropertyTableViewCell", bundle: nil),
                forCellReuseIdentifier: UITableViewCellReuseIdentifiers.property)
        }
        cell.setPropertyAndDelegate(property, delegate: delegate)
        return cell
    }
    
    func dequeueReusableDeviceTableViewCell(deviceInfo: DeviceInfo?, deviceInfoDetail: DeviceInfo.Detail?) -> DeviceTableViewCell {
        let cell: DeviceTableViewCell = doDequeue(UITableViewCellReuseIdentifiers.device) {
            $0.registerNib(UINib(nibName: "DeviceTableViewCell", bundle: nil),
                forCellReuseIdentifier: UITableViewCellReuseIdentifiers.device)
        }
        cell.setDeviceInfosAndDelegate(deviceInfo, deviceInfoDetail: deviceInfoDetail)
        return cell
    }
    
}