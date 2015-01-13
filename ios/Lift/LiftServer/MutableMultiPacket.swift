import Foundation

/**
 * Provides builder for the MultiPacket structure. Make an instance of it, then call
 * ``append`` as many times as you need, following by ``build()`` to get the 
 * ``NSData`` that can be sent & decoded on the server.
 */
class MutableMultiPacket {
    private var buffer: [SensorDataSourceLocation : NSMutableData] = [:]
    
    /**
     * Append ``data`` received from a sensor at ``location``. If there is already
     * value at the given ``location``, the passed ``data`` will be appended to it.
     */
    func append(location: SensorDataSourceLocation, data: NSData) -> Void {
        if let x = buffer[location] {
            x.appendData(data)
        } else {
            buffer[location] = NSMutableData(data: data)
        }
    }

    /**
     * Construct properly formed ``NSData`` from the values added to this instance.
     */
    func build() -> NSData {
        let result = NSMutableData()
        let header: [UInt8] = [0xca, 0xb0, UInt8(buffer.count)]
        result.appendBytes(header, length: 3)
        for (sdsl, data) in buffer {
            let sizel = UInt8(data.length & 0xff00 >> 8)
            let sizeh = UInt8(data.length >> 8)
            let dHeader: [UInt8] = [sizeh, sizel, sdsl.rawValue]
            result.appendBytes(dHeader, length: 3)
            result.appendData(data)
        }
        
        return result
    }
    
}