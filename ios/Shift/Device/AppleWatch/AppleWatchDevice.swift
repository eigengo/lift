class AppleWatchDevice : Device {
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "applewatch", error: NSError.notImplemented()))
    }
    
    func connect(deviceDelegate: DeviceDelegate, deviceSessionDelegate: DeviceSessionDelegate, onDone: ConnectedDevice -> Void) {
        // noop
    }
}
