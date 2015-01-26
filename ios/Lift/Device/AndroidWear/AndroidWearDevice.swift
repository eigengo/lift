class AndroidWearDevice : Device {
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "androidwear", location: .Wrist, error: NSError.notImplemented()))
    }
}
