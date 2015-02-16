import Foundation
import CoreMotion

extension CMAcceleration {
    
    ///
    /// Adds ``other`` to the vector
    ///
    func add(other: CMAcceleration) -> CMAcceleration {
        return CMAcceleration(x: x + other.x, y: y + other.y, z: z + other.z)
    }
    
}

extension CMDeviceMotion {
    
    ///
    /// Returns the actual measure accelerometer value.
    /// Core Motion infers gravity and removes that from
    /// the total accelerometer vector. But it is much more
    /// reliable to work from actual measured data than from
    //  data that is inferred, we reconstruct the actual
    /// measurement by adding the two together.
    /// Furthermore pebble cannot separate the two vectors,
    /// so this way the two accelerometer measures have
    /// the same shape.
    /// See:
    ///   - Core Motion doc: https://developer.apple.com/library/ios/documentation/CoreMotion/Reference/CMDeviceMotion_Class/index.html#//apple_ref/occ/instp/CMDeviceMotion/gravity
    ///
    var measuredAcceleration: CMAcceleration {
        get {
            return self.userAcceleration.add(self.gravity)
        }
    }
    
}
