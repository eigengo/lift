import Foundation

extension MutableMultiPacket {
    
    func appendPayload(location: DeviceInfo.Location, payload: Byte...) -> Self {
        return append(location, data: NSData(bytes: payload, length: payload.count))
    }
    
    func appendRepeatedPayload(location: DeviceInfo.Location, size: UInt16, const: UInt8) -> Self {
        let buffer: [UInt8] = [UInt8](count: Int(size), repeatedValue: const)
        return append(location, data: NSData(bytes: buffer, length: Int(size)))
    }

}