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
    
    func exists<A>(predicate: A -> Bool) -> Bool {
        for t in self {
            if let a = t as? A {
                if predicate(a) { return true }
            }
        }
        
        return false
    }
    
}