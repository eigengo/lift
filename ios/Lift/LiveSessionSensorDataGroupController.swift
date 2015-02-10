import Foundation

class LiveSessionSensorDataGroupController : UIViewController, MultiDeviceSessionSettable {
    private let secondWidth = 20.0
    @IBOutlet var sensorDataGroupView: SensorDataGroupView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    func multiDeviceSessionEncoding(session: MultiDeviceSession) {
        sensorDataGroupView.setSensorDataGroup(session.sensorDataGroup)
    }
    
    func mutliDeviceSession(session: MultiDeviceSession, continuousSensorDataEncodedBetween start: CFAbsoluteTime, and end: CFAbsoluteTime) {
        //sessionDataGroupView.
    }

}

class SensorDataGroupView : UIView {
    private var sensorDataGroup: SensorDataGroup?
    private let colors: [UIColor] = [
        UIColor(red: CGFloat(0.37), green: CGFloat(0.74), blue: CGFloat(0.95), alpha: CGFloat(1.0)),
        UIColor(red: CGFloat(0.33), green: CGFloat(0.44), blue: CGFloat(0.58), alpha: CGFloat(1.0)),
        UIColor(red: CGFloat(0.67), green: CGFloat(0.51), blue: CGFloat(0.96), alpha: CGFloat(1.0))
    ]
    
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
                    let frame = CGRect(x: x, y: y, width: w, height: height)
                    CGContextSetFillColorWithColor(ctx, colors[i % colors.count].CGColor)
                    CGContextFillRect(ctx, frame)

                    CGContextSetStrokeColorWithColor(ctx, UIColor.blueColor().CGColor)
                    CGContextSetLineWidth(ctx, 1.0)
                    CGContextStrokeRect(ctx, frame)
                }
                y += height + padding
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
