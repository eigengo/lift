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
    /// Updates the underlying session stats by looking up existing (or creating a zero) value for given ``key``, and
    /// then applying the ``update`` function to it.
    ///
    func updateStats(key: DeviceSessionStatsTypes.Key, update: DeviceSessionStatsTypes.Entry -> DeviceSessionStatsTypes.Entry) -> DeviceSessionStatsTypes.Entry {
        return stats.update(key, update: update)
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

