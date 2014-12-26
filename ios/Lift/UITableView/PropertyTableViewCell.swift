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

///
/// Basic property definition
///
struct PropertyDescriptor {
    var type: PropertyType
    var noneAllowed: Bool
    var title: String
    var value: String?
    
    init(title: String, value: String?) {
        self.init(title: title, type: .Text, noneAllowed: true, value: value)
    }
    
    init(title: String, type: PropertyType) {
        self.init(title: title, type: type, noneAllowed: true, value: nil)
    }
    
    init(title: String, type: PropertyType, value: String?) {
        self.init(title: title, type: type, noneAllowed: true, value: value)
    }
    
    init(title: String, type: PropertyType, noneAllowed: Bool, value: String?) {
        self.title = title
        self.value = value
        self.type = type
        self.noneAllowed = noneAllowed
    }
    
    func fold<A>(z: A, f: A -> String?) -> PropertyDescriptor {
        return PropertyDescriptor(title: title, type: type, noneAllowed: noneAllowed, value: f(z))
    }
}

/**
 * The property table cell delegate implementations must provide ways to access and set
 * the values referenced by the given property. The implementor should perform validation
 * and indicate validation errors as required
 */
protocol PropertyTableViewCellDelegate {
    /**
     * Called when the cell requires a value for the given property
     *
     * @param property the property name
     * @return the property descriptor
     */
    func propertyTableViewCellGetProperty(property: String) -> PropertyDescriptor
    
    /**
     * Called when the property has finished updating and now needs validation
     *
     * @param value the new value
     * @param property the property that's been changed
     * @return validation error, if any
     */
    func propertyTableViewCellValueValidate(value: String, property: String) -> String?
    
    /**
     * Called when the cell's value has changed.
     *
     * @param value the new value
     * @param property the property that's been changed
     */
    func propertyTableViewCellValueChanged(value: String, property: String)
}

internal struct PropertyTableViewCellImages {
    internal static let validationFailedImage = UIImage(named: "validationfailed")!
}

/**
 * UITableViewCell that can edit a property and save it into a model
 */
class PropertyTableViewCell : UITableViewCell {
    @IBOutlet var titleLabel: UILabel!
    @IBOutlet var editorTextField: UITextField!
    private var delegate: PropertyTableViewCellDelegate?
    private var property: String?
    private var descriptor: PropertyDescriptor?
    
    private func updateEditorTextFieldForType(type: PropertyType) {
        switch type {
        case .Integer(let min, let max): editorTextField.keyboardType = UIKeyboardType.NumberPad
        // TODO: other input types
        default: return
        }
    }
    
    private func autonomousValidationError() -> String? {
        switch descriptor!.type {
        case .Integer(let min, let max):
            if editorTextField.text.isEmpty && !descriptor!.noneAllowed {
                // Empty when value needed
                return "Property.emptyButValueNeeded".localized()
            }
            if !editorTextField.text.isEmpty {
                if let x = editorTextField.text.toInt() {
                    // we have value, check range
                    if x < min || x > max {
                        // out of range
                        return "Property.integerOutOfRange".localized(x, min, max)
                    }
                } else {
                    // not an int
                    return "Property.integerFormatError".localized(editorTextField.text)
                }
            }
        // TODO: other input types
        default: return nil
        }
        
        // no errors
        return nil
    }
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        selectionStyle = UITableViewCellSelectionStyle.None
    }

    func setPropertyAndDelegate(property: String, delegate: PropertyTableViewCellDelegate) {
        self.property = property
        self.delegate = delegate
        descriptor = delegate.propertyTableViewCellGetProperty(property)

        titleLabel.text = descriptor!.title
        editorTextField.text = descriptor!.value
        
        updateEditorTextFieldForType(descriptor!.type)
    }
    
    @IBAction func editorTextFieldEditingChanged() {
        if let p = property {
            if let x = autonomousValidationError() {
                editorTextField.rightView = UIImageView(image: PropertyTableViewCellImages.validationFailedImage)
                editorTextField.rightViewMode = UITextFieldViewMode.WhileEditing
                // we have failed autonomous validation
                return
            }
            if let x = delegate?.propertyTableViewCellValueValidate(editorTextField.text, property: p) {
                // validation failed
                editorTextField.rightView = UIImageView(image: PropertyTableViewCellImages.validationFailedImage)
                editorTextField.rightViewMode = UITextFieldViewMode.WhileEditing
                return
            }
            
            // all OK
            editorTextField.rightView = nil
            editorTextField.rightViewMode = UITextFieldViewMode.Never
            delegate?.propertyTableViewCellValueChanged(editorTextField.text, property: p)
        }
    }
}
