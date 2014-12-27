class AndroidWearDevice : Device {
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "androidwear", error: NSError.notImplemented()))
    }
}
