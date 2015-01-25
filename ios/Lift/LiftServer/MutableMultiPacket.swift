import Foundation
import QuartzCore

///
/// Saves the multi packet value
///
enum MutableMultiPacketEntry {
    case Data(data: NSMutableData)
    case Empty
}

/**
 * Provides builder for the MultiPacket structure. Make an instance of it, then call
 * ``append`` as many times as you need, following by ``build()`` to get the 
 * ``NSData`` that can be sent & decoded on the server.
 */
class MutableMultiPacket : MultiPacket {
    private var buffer: [SensorDataSourceLocation : MutableMultiPacketEntry] = [:]
    
    /**
     * Append ``data`` received from a sensor at ``location``. If there is already
     * value at the given ``location``, the passed ``data`` will be appended to it.
     */
    func append(location: SensorDataSourceLocation, data: NSData) -> Self {
        switch buffer[location] {
        case .Some(.Data(data: let x)): x.appendData(data)
        case .Some(.Empty): buffer[location] = MutableMultiPacketEntry.Data(data: NSMutableData(data: data))
        case .None: buffer[location] = MutableMultiPacketEntry.Data(data: NSMutableData(data: data))
        }
        
        return self
    }

    /**
     * Construct properly formed ``NSData`` from the values added to this instance.
     */
    func data() -> NSData {
        let result = NSMutableData()
        let header: [UInt8] = [0xca, 0xb1, UInt8(buffer.count)]
        result.appendBytes(header, length: 3)
        let timestampValue = Int(CACurrentMediaTime() * 1000)
        let timestampBuffer: [UInt8] = [
            UInt8((timestampValue & 0xff000000) >> 24),
            UInt8((timestampValue & 0x00ff0000) >> 16),
            UInt8((timestampValue & 0x0000ff00) >> 8),
            UInt8(timestampValue & 0x000000ff)]
        result.appendBytes(timestampBuffer, length: 4)
        
        for (sdsl, entry) in buffer {
            switch entry {
            case .Data(data: let data):
                let sizel = UInt8(data.length & 0xff00 >> 8)
                let sizeh = UInt8(data.length >> 8)
                let dHeader: [UInt8] = [sizeh, sizel, sdsl.rawValue]
                result.appendBytes(dHeader, length: 3)
                result.appendData(data)
            case .Empty:
                let sizel = UInt8(0)
                let sizeh = UInt8(0)
                let dHeader: [UInt8] = [sizeh, sizel, sdsl.rawValue]
                result.appendBytes(dHeader, length: 3)
            }
        }
        
        return result
    }
    
}
