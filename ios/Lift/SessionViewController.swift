import UIKit

/* Cells in table view can either have a description or an image. Demonstrates
an algebraic data type. */
enum CellData {
    case ActiveCell(session: UInt32, bytesReceived: UInt, bytesPerSecond: Double)
    case EndedCell(session: UInt32, bytesReceived: UInt)
    
    var session: UInt32 {
        get {
            switch self {
            case .ActiveCell(let tag, _, _): return tag
            case .EndedCell(let tag, _): return tag
            }
        }
    }
    
    func end() -> CellData {
        switch self {
        case .ActiveCell(let t, let br, _): return CellData.EndedCell(session: t, bytesReceived: br)
        case .EndedCell(let t, let br): return CellData.EndedCell(session: t, bytesReceived: br)
        }
    }
}

class SessionViewController: UITableViewController, UITableViewDelegate, UITableViewDataSource, AccelerometerReceiverDelegate {
    private let receiver = PebbleAccelerometerReceiver()
    private let recorder = CombinedAccelerometerRecorder(recorders: [LocalAccelerometerRecorder(), PEAccelerometerRecorder()])
    private var sessionCells: [CellData] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        receiver.delegate = self
    }
    
    @IBAction func poll(AnyObject) {
        PBPebbleCentral.defaultCentral().dataLoggingService.pollForData()
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
            cell.textLabel.text = String(format: "%0x", tag)
            cell.detailTextLabel!.text = String(format: "Received %d, bps: %f", bytesReceived, bytesPerSecond)
        case .EndedCell(let tag, let bytesReceived):
            cell.textLabel.text = String(format: "%0x", tag)
            cell.detailTextLabel!.text = String(format: "Received %d", bytesReceived)
        }
        
        return cell
    }

    
    func accelerometerReceiverEnded(session: UInt32, stats: AccelerometerSessionStats?) {
        self.sessionCells = self.sessionCells.map { (data: CellData) -> CellData in
            if data.session == session {
                return data.end()
            } else {
                return data
            }
        }
        
        self.recorder.accelerometerReceiverEnded(session, stats: stats)
        tableView.reloadData()
    }
    
    func accelerometerReceiverReceived(data: NSData, session: UInt32, stats: AccelerometerSessionStats?) {
        self.recorder.accelerometerReceiverReceived(data, session: session, stats: stats)
        
        if stats == nil {
            return
        }
        
        if !contains(self.sessionCells, { (data: CellData) -> Bool in return data.session == session}) {
            self.sessionCells.append(CellData.ActiveCell(session: session, bytesReceived: stats!.bytesReceived, bytesPerSecond: stats!.bytesPerSecond))
        } else {
            self.sessionCells = self.sessionCells.map { (data: CellData) -> CellData in
                switch data {
                case .ActiveCell(session, _, _):
                    return CellData.ActiveCell(session: session, bytesReceived: stats!.bytesReceived, bytesPerSecond: stats!.bytesPerSecond)
                default:
                    return data
                }
            }
        }
        tableView.reloadData()
    }
        
}
