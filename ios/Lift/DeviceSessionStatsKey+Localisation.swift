import Foundation

extension DeviceSessionStatsKey {
    
    func localized() -> String {
        switch self {
        case .Accelerometer: return "DeviceSessionStatsKey.Accelerometer".localized();
        }
    }

}