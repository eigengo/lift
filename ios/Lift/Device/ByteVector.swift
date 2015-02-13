import Foundation

///
/// Encapsulates an array of bytes, provides methods
/// for easier processing
///
class ByteVector {
    
    let bytes: [UInt8]
    var currentPosition = 0
    
    init(_ bytes: [UInt8]) {
        self.bytes = bytes
    }
    
    ///
    /// Returns the number of bytes in the vectors
    ///
    var count: Int {
        get {
            return bytes.count
        }
    }
    
    ///
    /// Reads 3 accelerometer values (x, y, z) and
    /// increments ``currentPosition``
    ///
    /* mutating */
    func readInt13s() -> [Int16] {
        let packedStructureLength = 5
        assert(currentPosition + packedStructureLength <= bytes.count)
        let result = bytes.withUnsafeBufferPointer { (pointer: UnsafeBufferPointer<UInt8>) -> [Int16] in
            var x: Int16 = 0
            var y: Int16 = 0
            var z: Int16 = 0
            let pointerAtOffset = UnsafePointer<()>(pointer.baseAddress).advancedBy(self.currentPosition)
            decode_lift_accelerometer_data(pointerAtOffset, &x, &y, &z)
            return [x, y, z]
        }
        currentPosition += packedStructureLength
        return result
    }
    
}
