import Foundation

extension Array {
    
    mutating func removeObject<U: Equatable>(object: U) {
        var index: Int?
        for (idx, objectToCompare) in enumerate(self) {
            if let to = objectToCompare as? U {
                if object == to {
                    index = idx
                }
            }
        }
        
        if(index != nil) {
            self.removeAtIndex(index!)
        }
    }
    
    ///
    /// Finds the first element that satisfies ``predicate``
    ///
    func find(predicate: Element -> Bool) -> Element? {
        for e in self {
            if predicate(e) { return e }
        }
        
        return nil
    }
    
    ///
    /// Returns ``true`` if ``predicate`` evaluates to ``true`` for all elements.
    ///
    func forall(predicate: Element -> Bool) -> Bool {
        for e in self {
            if !predicate(e) { return false }
        }
        
        return true
    }
    
    ///
    /// Returns ``true`` if the ``predicate`` evalues to ``true`` for at least
    /// one element in this array
    ///
    func exists(predicate: Element -> Bool) -> Bool {
        return find(predicate) != nil
    }
    
}