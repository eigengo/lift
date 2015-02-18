import Foundation

///
/// Session receiving app updates from the watch. When initialised with ``PBWatch``, it sets all required handlers
/// and receives the incoming data from the device.
///
/// It also maintains required stats.
///
class PebbleDeviceSession : DeviceSession {
    private var deviceSessionDelegate: DeviceSessionDelegate!
    private var startTime: NSDate!
    private var updateHandler: AnyObject?
    private var watch: PBWatch!
    private var deviceId: NSUUID!
    
    required init(deviceId: NSUUID, watch: PBWatch!, deviceSessionDelegate: DeviceSessionDelegate) {
        super.init()
        self.watch = watch
        self.deviceId = deviceId
        self.startTime = NSDate()
        self.deviceSessionDelegate = deviceSessionDelegate
        self.updateHandler = watch.appMessagesAddReceiveUpdateHandler(appMessagesReceiveUpdateHandler)
    }
    
    // MARK: main

    override func zero() -> NSTimeInterval {
        // ???
        zeroStats()
        NSLog("INFO: PebbleDeviceSession zero()")
        return 0.0  // Real implementation should tell the watch to reset. We just ignore and thus we took 0 ms.
    }
    
    override func stop() {
        if let x: AnyObject = updateHandler { watch.appMessagesRemoveUpdateHandler(x) }
        deviceSessionDelegate.deviceSession(self, endedFrom: deviceId)
    }
    
    private func appMessagesReceiveUpdateHandler(watch: PBWatch!, data: [NSObject : AnyObject]!) -> Bool {
        let adKey = NSNumber(uint32: 0xface0fb0)
        let deadKey = NSNumber(uint32: 0x0000dead)
        if let x = data[adKey] as? NSData {
            accelerometerDataReceived(x)
        }
        if let x: AnyObject = data[deadKey] {
            stop()
        }
        return true
    }
    
    private func accelerometerDataReceived(data: NSData) {
        updateStats(DeviceSessionStatsTypes.Key(sensorKind: .Accelerometer, deviceId: deviceId), update: { prev in
            return DeviceSessionStatsTypes.Entry(bytes: prev.bytes + data.length, packets: prev.packets + 1)
        })
        
        deviceSessionDelegate.deviceSession(self, sensorDataReceivedFrom: deviceId, atDeviceTime: CFAbsoluteTimeGetCurrent(), data: data)
    }
}
