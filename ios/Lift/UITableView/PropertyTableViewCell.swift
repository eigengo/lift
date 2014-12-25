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
 * The property table cell delegate implementations must provide ways to access and set
 * the values referenced by the given property. The implementor should perform validation
 * and indicate validation errors as required
 */
protocol PropertyTableViewCellDelegate {
    /**
     * Called when the cell requires a value for the given property
     *
     * @param property the property name
     * @return the property value as string
     */
    func propertyTableViewCellGetValueForProperty(property: String) -> String?
    
    /**
     * Compute the title for the given property
     *
     * @param property the property name
     * @return the title
     */
    func propertyTableViewCellGetTitleForProperty(property: String) -> String
    
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

/**
 * UITableViewCell that can edit a property and save it into a model
 */
class PropertyTableViewCell : UITableViewCell {
    @IBOutlet var titleLabel: UILabel!
    @IBOutlet var editorTextField: UITextField!
    private var delegate: PropertyTableViewCellDelegate?
    private var property: String?
    
    func setPropertyAndDelegate(property: String, delegate: PropertyTableViewCellDelegate) {
        self.property = property
        self.delegate = delegate
        titleLabel.text = delegate.propertyTableViewCellGetTitleForProperty(property)
        editorTextField.text = delegate.propertyTableViewCellGetValueForProperty(property)
    }
    
    @IBAction func editorTextFieldEditingChanged() {
        if let p = property {
            if let x = delegate?.propertyTableViewCellValueValidate(editorTextField.text, property: p) {
                // validation failed
            } else {
                delegate?.propertyTableViewCellValueChanged(editorTextField.text, property: p)
            }
        }
    }
}
