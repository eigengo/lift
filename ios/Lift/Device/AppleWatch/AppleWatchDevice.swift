class AppleWatchDevice : Device {
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "applewatch", error: NSError.notImplemented()))
    }
}
