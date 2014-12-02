enum PEAccelerometerRecorderURLs: URLStringConvertible {
    static let baseURLString = "http://localhost:8080"
    
    case AccelerometerData
    
    // MARK: URLStringConvertible
    var URLString: String {
        get {
            let path: String = {
                switch self {
                case .AccelerometerData:
                    return "/exercise"
                }
            }()
            
            return PEAccelerometerRecorderURLs.baseURLString + path
        }
    }
}

class PEAccelerometerRecorder : NSObject, AccelerometerReceiverDelegate {
    
    func accelerometerReceiverEnded(session: UInt32, stats: AccelerometerSessionStats?) {
        
    }
    
    func accelerometerReceiverReceived(data: NSData, session: UInt32, stats: AccelerometerSessionStats?) {
        upload(.POST, PEAccelerometerRecorderURLs.AccelerometerData, data)
    }
    
}