import Foundation

///
/// Adds big endian appending functionality
///
extension NSMutableData {

    ///
    /// Encodes the ``value`` as big-endian array of bytes
    ///
    private func asBigEndian(value: UInt16) -> [UInt8] {
        return [
            (UInt8)((value & 0xff00) >> 8),
            (UInt8)(value & 0x00ff)
        ]
    }
    
    ///
    /// Encodes the ``value`` as big-endian array of bytes
    ///
    private func asBigEndian(value: UInt32) -> [UInt8] {
        return [
            (UInt8)((value & 0xff000000) >> 24),
            (UInt8)((value & 0x00ff0000) >> 16),
            (UInt8)((value & 0x0000ff00) >> 8),
            (UInt8) (value & 0x000000ff)
        ]
    }
    
    ///
    /// Appends the given bytes to this data
    ///
    func appendBytes(value: [UInt8]) -> Void {
        var bytes = value
        self.appendBytes(&bytes, length: value.count)
    }
    
    ///
    /// Appends a single byte in ``value``
    ///
    func appendUInt8(value: UInt8) -> Void {
        self.appendBytes([value])
    }
    
    ///
    /// Appends 16bit unsigned ``value``
    ///
    func appendUInt16(value: UInt16) -> Void {
        self.appendBytes(asBigEndian(value))
    }
    
    ///
    /// Appends 32bit unsigned ``value``
    ///
    func appendUInt32(value: UInt32) -> Void {
        self.appendBytes(asBigEndian(value))
    }
    
}