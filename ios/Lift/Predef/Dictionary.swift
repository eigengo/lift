import Foundation

extension Dictionary {
    
    /**
    * Return the session stats as a list of tuples, ordered by the key
    */
    func toList() -> [(Key, Value)] {
        var r: [(Key, Value)] = []
        for (k, v) in self {
            let tuple = [(k, v)]
            r += tuple
        }
        return r
    }

}