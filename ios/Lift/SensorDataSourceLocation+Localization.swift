import Foundation

extension DeviceInfo.Location {
    
    func localized() -> String {
        switch self {
        case Wrist: return "Wrist".localized()
    	case Waist: return "Waist".localized()
    	case Chest: return "Chest".localized()
    	case Foot:  return "Foot".localized()
    	case Any:   return "Any".localized()
        }
    }
    
}