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
    
    func localisedDescription() -> String {
        switch self {
        case Wrist: return "WristDescription".localized()
        case Waist: return "WaistDescription".localized()
        case Chest: return "ChestDescription".localized()
        case Foot:  return "FootDescription".localized()
        case Any:   return "AnyDescription".localized()
        }
    }
    
}