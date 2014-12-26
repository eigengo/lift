class AppleWatchDevice : Device {
    func start() {
        // TODO: implement me
    }
    
    func stop() {
        // TODO: implement me
    }
    
    func peek(onDone: (Either<(NSError, DeviceType), DeviceInfo>) -> Void) {
        onDone(Either.left(NSError.errorWithMessage("Not implemented", code: 666), "applewatch"))
    }
}
