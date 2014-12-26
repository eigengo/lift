class AppleWatchDevice : Device {
    func start() {
        // TODO: implement me
    }
    
    func stop() {
        // TODO: implement me
    }
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "applewatch", error: NSError.errorWithMessage("Not implemented", code: 666)))
    }
}
