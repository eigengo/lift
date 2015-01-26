class AppleWatchDevice : Device {
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "applewatch", location: .Wrist, error: NSError.notImplemented()))
    }
}
