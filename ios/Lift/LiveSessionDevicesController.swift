import UIKit

class LiveSessionDevicesController: UITableViewController, UITableViewDelegate, UITableViewDataSource, MultiDeviceSessionSettable {
    private var exerciseSession: ExerciseSession?
    private var deviceInfos: [ConnectedDeviceInfo] = []
    private var sessionStats: [(DeviceSessionStatsTypes.KeyWithLocation, DeviceSessionStatsTypes.Entry)] = []
    
    // MARK: MultiDeviceSessionSettable
    func multiDeviceSessionEncoding(session: MultiDeviceSession) {
        deviceInfos = session.getDeviceInfos()
        sessionStats = session.getSessionStats()
        tableView.reloadData()
    }
    
    func mutliDeviceSession(session: MultiDeviceSession, continuousSensorDataEncodedBetween start: CFAbsoluteTime, and end: CFAbsoluteTime) {
        
    }
    
    // MARK: UITableViewDataSource
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2 // section 1: device & session, section 2: exercise log
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return deviceInfos.count
        case 1: return sessionStats.count
        default: return 0
        }
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        switch (indexPath.section, indexPath.row) {
            // section 1: device
        case (0, let x):
            let cdi = deviceInfos[x]
            return tableView.dequeueReusableDeviceTableViewCell(cdi.deviceInfo, deviceInfoDetail: cdi.deviceInfoDetail)
            // section 2: sensors
        case (1, let x):
            let (key, stats) = sessionStats[x]
            let cell = tableView.dequeueReusableCellWithIdentifier("sensor") as UITableViewCell
            cell.textLabel!.text = key.location.localized() + " " + key.sensorKind.localized()
            cell.detailTextLabel!.text = "LiveSessionDevicesController.stats".localized(stats.bytes, stats.packets)
            return cell
            // section 2: exercise log
        default: return UITableViewCell()
        }
    }
    
    override func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0: return "Devices".localized()
        case 1: return "Sensors".localized()
        default: return ""
        }
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        switch indexPath.section {
        case 0: return 60
        default: return 40
        }
    }
    
}
