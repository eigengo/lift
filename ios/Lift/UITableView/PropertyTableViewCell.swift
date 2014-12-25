import Foundation

/**
 * ADT for property types
 */
enum PropertyType {
    /// arbitrary text
    case Text
    
    /// integer between the min and max
    case Integer(Int, Int)
    
    /// date (no time)
    case Date
}

/**
 * UITableViewCell that can edit a property and save it into a model
 */
class PropertyTableViewCell : UITableViewCell {
    @IBOutlet var titleLabel: UILabel!
    @IBOutlet var editorTextField: UITextField!
    private var property: String?
    private var type: PropertyType?
    private var empty: AnyObject?
    
    private func convertValue() -> Either<String, AnyObject> {
        switch type! {
        case .Text: return Either.right(editorTextField.text)
        case .Integer(let min, let max):
            if let i = editorTextField.text.toInt() {
                if i >= min && i <= max {
                    return Either.right(NSInteger(i))
                } else {
                    return Either.left("PropertyTableViewCell.integerOutOfRange".localized(min, max, i))
                }
            } else {
                return Either.left("PropertyTableViewCell.integerNotInteger".localized(editorTextField.text))
            }
        case .Date:
            return Either.left("Implement me")
        }
    }

    func merge(model: [String : AnyObject]) -> Either<String, [String : AnyObject]> {
        if (!editorTextField.text.isEmpty) {
            return convertValue().map { x in
                var m = model
                m[self.property!] = x
                return m
            }
        } else {
            var m = model
            m[property!] = empty!
            return Either.right(m)
        }
    }
    
    func load(model: [String : AnyObject], type: PropertyType, empty: AnyObject) {
        self.type = type
        self.empty = empty
    }
    
}
