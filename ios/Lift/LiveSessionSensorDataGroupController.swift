import Foundation

class LiveSessionSensorDataGroupController : UIViewController, MultiDeviceSessionSettable {
    private let secondWidth = 10.0
    private var layer: SensorDataGroupLayer!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        layer = SensorDataGroupLayer()
        view.layer.addSublayer(layer)
    }
    
    func setMultiDeviceSession(multi: MultiDeviceSession) {
        layer.frame = view.frame
        println(multi.sensorDataGroup.startTime)
        layer.setSensorDataGroup(multi.sensorDataGroup)
        
        /*
        let sdg = multi.sensorDataGroup
        let mst = sdg.startTime
        let met = sdg.endTime
        if mst != nil && met != nil {
            let st = mst!
            let et = met!
            
            let width = (et - st) * secondWidth

            view.subviews.foreach { x in
                if let v = x as? UIView {
                    v.removeFromSuperview()
                }
            }
            
            let height = 40.0
            let padding = 4.0
            var y: Double = padding
            for (i, sda) in enumerate(sdg.sensorDataArrays) {
                for (j, sd) in enumerate(sda.sensorDatas) {
                    let x = (sd.startTime - st) * secondWidth
                    let w = (sd.endTime(sda.header.sampleSize, samplesPerSecond: sda.header.samplesPerSecond) - st) * secondWidth
                    y += height + padding
                    let frame = CGRect(x: x, y: y, width: w, height: height)
                    view.addSubview(SensorDataArrayView(frame: frame))
                }
            }
        }
        */
    }
    
    private class SensorDataGroupLayer : CALayer {
        var sensorDataGroup: SensorDataGroup?
        
        func setSensorDataGroup(sensorDataGroup: SensorDataGroup) {
            self.sensorDataGroup = sensorDataGroup
            setNeedsDisplay()
        }
        
        override func drawInContext(ctx: CGContext!) {
            func drawLayer(sdg: SensorDataGroup, startTime: CFAbsoluteTime) {
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
                        CGContextSetFillColorWithColor(ctx, UIColor.blueColor().CGColor)
                        CGContextAddRect(ctx, frame)
                    }
                }
                
            }
            
            if let sdg = sensorDataGroup {
                if let st = sdg.startTime {
                    drawLayer(sdg, st)
                }
            }
        }
        
    }
    
    private class SensorDataArrayView : UIView {
        override init(frame: CGRect) {
            super.init(frame: frame)
            backgroundColor = UIColor.blueColor()
            layer.borderColor = UIColor.grayColor().CGColor
        }

        required init(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
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

