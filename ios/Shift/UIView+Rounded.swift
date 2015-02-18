import Foundation

extension UIView {
    
    func roundedBorder(color: UIColor) {
        layer.borderWidth = 1.5
        layer.cornerRadius = 3
        layer.borderColor = color.CGColor
        clipsToBounds = true
    }
    
}