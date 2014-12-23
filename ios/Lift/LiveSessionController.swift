import UIKit

class LiveSessionController: UITableViewController, UITableViewDelegate, UITableViewDataSource, AccelerometerReceiverDelegate, ExerciseSessionSettable {
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    func setExerciseSession(session: ExerciseSession) {
        PebbleAccelerometerReceiver(delegate: self)
        NSLog("Starting with %@", session)
    }
    
    // #pragma mark - UITableViewDataSource
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sessionCells.count
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell  {
        let data = sessionCells[indexPath.row]
        let cell = UITableViewCell(style: .Subtitle, reuseIdentifier: nil)
        
        switch(data) {
        case .ActiveCell(let tag, let bytesReceived, let bytesPerSecond):
            cell.textLabel!.text = String(format: "%0x", tag)
            cell.detailTextLabel!.text = String(format: "Received %d, bps: %f", bytesReceived, bytesPerSecond)
        case .EndedCell(let tag, let bytesReceived):
            cell.textLabel!.text = String(format: "%0x", tag)
            cell.detailTextLabel!.text = String(format: "Received %d", bytesReceived)
        }
        
        return cell
    }

    func accelerometerReceiverReceived(deviceSession: NSUUID, data: NSData, stats: AccelerometerSessionStats) {

        tableView.reloadData()
    }
    
    func accelerometerReceiverEnded(deviceSession: NSUUID, stats: AccelerometerSessionStats?) {
        tableView.reloadData()
    }
        
}
