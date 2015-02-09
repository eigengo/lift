import Foundation

class LiveSessionSensorDataGroupController : UIViewController, MultiDeviceSessionSettable {
    private let secondWidth = 10.0
    @IBOutlet var sensorDataGroupView: SensorDataGroupView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    func setMultiDeviceSession(multi: MultiDeviceSession) {
        sensorDataGroupView.setSensorDataGroup(multi.sensorDataGroup)
    }
    
}

extension SensorDataGroup {

    ///
    /// Returns the start time of the earliest block of data
    ///
    var startTime: CFAbsoluteTime? {
        get {
            return sensorDataArrays.flatMap { $0.startTime }.minBy(identity)
        }
    }
    
    ///
    /// Returns the end time of the last block of data
    ///
    var endTime: CFAbsoluteTime? {
        get {
            return sensorDataArrays.flatMap { $0.endTime }.maxBy(identity)
        }
    }
    
}


extension SensorDataArray {
    
    ///
    /// Returns the start time of the earliest block of data
    ///
    var startTime: CFAbsoluteTime? {
        get {
            return sensorDatas.map { $0.startTime }.minBy(identity)
        }
    }
    
    ///
    /// Returns the end time of the last block of data
    ///
    var endTime: CFAbsoluteTime? {
        get {
            return sensorDatas.map { $0.endTime(self.header.sampleSize, samplesPerSecond: self.header.samplesPerSecond) }.maxBy(identity)
        }
    }
    
}

class SensorDataGroupView : UIView {
    var sensorDataGroup: SensorDataGroup?
    
    func setSensorDataGroup(sensorDataGroup: SensorDataGroup) {
        self.sensorDataGroup = sensorDataGroup
        setNeedsDisplay()
    }
    
    override func drawRect(rect: CGRect) {
        func drawLayer(ctx: CGContextRef, sdg: SensorDataGroup, startTime: CFAbsoluteTime) {
            let height = 40.0
            let padding = 4.0
            let secondWidth = 20.0
            var y: Double = padding
            for (i, sda) in enumerate(sdg.sensorDataArrays) {
                for (j, sd) in enumerate(sda.sensorDatas) {
                    let x = (sd.startTime - startTime) * secondWidth
                    let w = (sd.endTime(sda.header.sampleSize, samplesPerSecond: sda.header.samplesPerSecond) - startTime) * secondWidth
                    y += height + padding
                    let frame = CGRect(x: x, y: y, width: w, height: height)
                    println(frame)
                    CGContextSetFillColorWithColor(ctx, UIColor.blueColor().CGColor)
                    CGContextFillRect(ctx, frame)
                }
            }
            
        }
        
        
        let context = UIGraphicsGetCurrentContext()
        if let sdg = sensorDataGroup {
            if let st = sdg.startTime {
                drawLayer(context, sdg, st)
            }
        }
    }
    
}
