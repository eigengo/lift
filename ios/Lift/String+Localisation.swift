import Foundation

extension String {
    
    func localized(args: CVarArgType...) -> String {
        let s = NSLocalizedString(self, comment: self)
        if args.count == 0 {
            return s
        } else {
            return String(format: s, arguments: args)
        }
    }
    
}
