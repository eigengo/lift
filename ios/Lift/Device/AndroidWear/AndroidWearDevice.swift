class AndroidWearDevice : Device {
    
    func peek(onDone: DeviceInfo -> Void) {
        onDone(DeviceInfo.NotAvailableDeviceInfo(type: "androidwear", error: NSError.notImplemented()))
    }
    
    func connect(deviceDelegate: DeviceDelegate, sensorDataDelegate: SensorDataDelegate, onDone: ConnectedDevice -> Void) {
        // noop
    }
}
