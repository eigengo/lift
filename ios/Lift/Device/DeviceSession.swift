import Foundation

///
/// DeviceSessionId type
///
typealias DeviceSessionId = NSUUID

///
/// The exercise session connected to the device
///
class DeviceSession {
    private var id: DeviceSessionId!
    private let stats = DeviceSessionStats<DeviceSessionStatsTypes.Key>()
    
    ///
    /// Constructs a new session with generated identity
    ///
    init() {
        self.id = DeviceSessionId()
    }

    ///
    /// Implementations must override this to handle stopping of the session
    ///
    func stop() -> Void {
        fatalError("Implement me")
    }
    
    ///
    /// Tells the device to potentially drop partially recorded data set, and reset the recording to zero. The communication layer
    /// and the device should do all they can to comply with the request, and return a ``NSTimeInterval`` indicating offset from
    /// the time the request was received to the time it took to fulfil it.
    ///
    /// If the time cannot be reliably measured, it will be sufficient to report average time to fulfilment
    ///
    func zero() -> NSTimeInterval {
        fatalError("Implement me")
    }

    ///
    /// Updates the underlying session stats by looking up existing (or creating a zero) value for given ``key``, and
    /// then applying the ``update`` function to it.
    ///
    func updateStats(key: DeviceSessionStatsTypes.Key, update: DeviceSessionStatsTypes.Entry -> DeviceSessionStatsTypes.Entry) -> DeviceSessionStatsTypes.Entry {
        return stats.update(key, update: update)
    }
    
    ///
    /// Zeros out current stats
    ///
    func zeroStats() {
        stats.zero()
    }

    ///
    /// Return the session identity
    ///
    final func sessionId() -> DeviceSessionId {
        return id
    }
    
    ///
    /// Gets the session stats
    ///
    final func getStats() -> DeviceSessionStats<DeviceSessionStatsTypes.Key> {
        return stats
    }
    
}

