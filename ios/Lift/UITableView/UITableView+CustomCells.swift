import Foundation

struct UITableViewCellReuseIdentifiers {
    internal static let device = "__device"
    internal static let property = "__property"
}

/**
 * Convenience extension to UITableViewController to allow the custom cells to be dequeued
 * by loading them from their matching nibs
 */
extension UIViewController {
    
    private func doDequeue<A where A : UITableViewCell>(tableView: UITableView, reuseIdentifier: String, register: UITableView -> Void) -> A {
        let cell = tableView.dequeueReusableCellWithIdentifier(reuseIdentifier) as A?
        
        if let x = cell { return x } else {
            register(tableView)
            return tableView.dequeueReusableCellWithIdentifier(reuseIdentifier) as A
        }
    }
    
    func dequeueReusablePropertyTableViewCell(tableView: UITableView, property: String, delegate: PropertyTableViewCellDelegate) -> PropertyTableViewCell {
        let cell: PropertyTableViewCell = doDequeue(tableView, reuseIdentifier: UITableViewCellReuseIdentifiers.property) {
            $0.registerNib(UINib(nibName: "PropertyTableViewCell", bundle: nil),
                forCellReuseIdentifier: UITableViewCellReuseIdentifiers.property)
        }
        cell.setPropertyAndDelegate(property, delegate: delegate)
        return cell
    }
    
    func dequeueReusableDeviceTableViewCell(tableView: UITableView, deviceInfo: DeviceInfo, deviceInfoDetail: DeviceInfo.Detail?) -> DeviceTableViewCell {
        let cell: DeviceTableViewCell = doDequeue(tableView, reuseIdentifier: UITableViewCellReuseIdentifiers.device) {
            $0.registerNib(UINib(nibName: "DeviceTableViewCell", bundle: nil),
                forCellReuseIdentifier: UITableViewCellReuseIdentifiers.device)
        }
        cell.setDeviceInfo(deviceInfo, deviceInfoDetail: deviceInfoDetail)
        return cell
    }
    
}