import Foundation

extension NSData {
    
    ///
    /// Views this NSData as array of bytes
    ///
    func asBytes() -> [UInt8] {
        var buf = [UInt8](count: length, repeatedValue: 0)
        getBytes(&buf, length: length)
        return buf
    }
    
    ///
    /// Views this data as a ByteVector
    ///
    func asByteVector() -> ByteVector {
        return ByteVector(self.asBytes())
    }
    
    ///
    /// Views this data as an array of 13 bit integers
    ///
    func asInt13s() -> [Int16] {
        var vector = self.asByteVector()
        let count = vector.count / 5
        var result = [Int16](count: (count * 3), repeatedValue: 0)
        for i in 0..<count {
            let current = i * 3
            let values = vector.readInt13s()
            result[current...(current + 2)] = values[0...2]
        }
        return result
    }
    
}