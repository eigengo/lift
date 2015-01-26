import Foundation

extension DeviceSessionStats.SensorKind {
    
    func localized() -> String {
        switch self {
        case .Accelerometer: return "DeviceSessionStatsKey.Accelerometer".localized();
        case .Gyroscope: return "DeviceSessionStatsKey.Gyroscope".localized();
        case .GPS: return "DeviceSessionStatsKey.GPS".localized();
        case .HeartRate: return "DeviceSessionStatsKey.HeartRate".localized();
        }
    }

}
