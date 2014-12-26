class FitbitDevice : Device {
    func start() {
        // TODO: implement me
    }
    
    func stop() {
        // TODO: implement me
    }
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "fitbit", error: NSError.errorWithMessage("Not implemented", code: 666)))
    }
}
