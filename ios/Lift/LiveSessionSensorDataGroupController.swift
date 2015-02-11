import Foundation

class LiveSessionSensorDataGroupController : UIViewController, MultiDeviceSessionSettable {
    private let secondWidth = 20.0
    @IBOutlet var sensorDataGroupView: SensorDataGroupView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        sensorDataGroupView.backgroundColor = UIColor.groupTableViewBackgroundColor()
    }
    
    func multiDeviceSessionEncoding(session: MultiDeviceSession) {
        sensorDataGroupView.setSensorDataGroup(session.sensorDataGroup)
    }
    
    func mutliDeviceSession(session: MultiDeviceSession, continuousSensorDataEncodedBetween start: CFAbsoluteTime, and end: CFAbsoluteTime) {
        sensorDataGroupView.setLastEncodedTimeRange(TimeRange(start: start, end: end))
    }

}

class SensorDataGroupView : UIView {
    private var sensorDataGroup: SensorDataGroup?
    private var lastEncodedTimeRange: TimeRange?
    private let secondWidth = 40.0
    
    private let colors: [UIColor] = [
        UIColor(red: CGFloat(0.37), green: CGFloat(0.74), blue: CGFloat(0.95), alpha: CGFloat(1.0)),
        UIColor(red: CGFloat(0.33), green: CGFloat(0.44), blue: CGFloat(0.58), alpha: CGFloat(1.0)),
        UIColor(red: CGFloat(0.67), green: CGFloat(0.51), blue: CGFloat(0.96), alpha: CGFloat(1.0))
    ]
    
    func setSensorDataGroup(sensorDataGroup: SensorDataGroup) {
        self.sensorDataGroup = sensorDataGroup
        setNeedsDisplay()
    }
    
    func setLastEncodedTimeRange(range: TimeRange) {
        lastEncodedTimeRange = range
        setNeedsDisplay()
    }
    
    override func drawRect(rect: CGRect) {
        
        func drawTimeGrid(ctx: CGContextRef) {
            let dashArray: [CGFloat] = [2, 2, 2, 2]
            let seconds = Int(frame.width / CGFloat(secondWidth))
            CGContextSaveGState(ctx)
            CGContextSetStrokeColorWithColor(ctx, UIColor.grayColor().CGColor)
            CGContextSetLineWidth(ctx, 0.5)
            CGContextSetLineDash(ctx, 3, dashArray, 4)
            
            for second in 0...seconds {
                let x = second * Int(secondWidth)
                
                CGContextBeginPath(ctx)
                CGContextMoveToPoint(ctx, CGFloat(x), 0)
                CGContextAddLineToPoint(ctx, CGFloat(x), frame.height)
                CGContextStrokePath(ctx)
            }
            
            CGContextRestoreGState(ctx)
        }
        
        func drawCurrentTime(ctx: CGContextRef, startTime: CFAbsoluteTime) {
            let currentTime = CFAbsoluteTimeGetCurrent()
            let x = (currentTime - startTime) * secondWidth
            
            CGContextSetStrokeColorWithColor(ctx, UIColor.blackColor().CGColor)
            CGContextSetLineWidth(ctx, 0.5)
            CGContextBeginPath(ctx)
            CGContextMoveToPoint(ctx, CGFloat(x), 0)
            CGContextAddLineToPoint(ctx, CGFloat(x), frame.height)
            CGContextStrokePath(ctx)
        }
        
        func drawLayer(ctx: CGContextRef, sdg: SensorDataGroup, startTime: CFAbsoluteTime) {
            let height = 40.0
            let padding = 4.0
            var y: Double = padding
            for (i, sda) in enumerate(sdg.sensorDataArrays) {
                for (j, sd) in enumerate(sda.sensorDatas) {
                    let x = (sd.startTime - startTime) * secondWidth
                    let end = (sd.endTime(sda.header.sampleSize, samplesPerSecond: sda.header.samplesPerSecond) - startTime) * secondWidth
                    let w = end - x
                    let frame = CGRect(x: x, y: y, width: w, height: height)
                    CGContextSetFillColorWithColor(ctx, colors[i % colors.count].CGColor)
                    CGContextFillRect(ctx, frame)

                    CGContextSetStrokeColorWithColor(ctx, UIColor.blueColor().CGColor)
                    CGContextSetLineWidth(ctx, 1.0)
                    CGContextStrokeRect(ctx, frame)
                }
                y += height + padding
            }

            if let range = lastEncodedTimeRange {
                let x = (range.start - startTime) * secondWidth
                let end = (range.end - startTime) * secondWidth
                let w = end - x
                let rect = CGRect(x: x, y: 0, width: w, height: y)
                CGContextSetFillColorWithColor(ctx, UIColor(red: 1, green: 0, blue: 0, alpha: 0.3).CGColor)
                CGContextFillRect(ctx, rect)
                CGContextSetStrokeColorWithColor(ctx, UIColor.redColor().CGColor)
                CGContextStrokeRect(ctx, rect)
                
                lastEncodedTimeRange = nil
            }
        }
        
        let context = UIGraphicsGetCurrentContext()
        if let sdg = sensorDataGroup {
            if let st = sdg.startTime {
                let offset = 2.0
                drawLayer(context, sdg, st - offset)
                drawTimeGrid(context)
                drawCurrentTime(context, st - offset)
            }
        }
    }
    
}
