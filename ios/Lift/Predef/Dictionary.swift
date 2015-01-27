import Foundation

extension Dictionary {
    
    ///
    /// Return the session stats as a list of tuples, ordered by the key
    ///
    func toList() -> [(Key, Value)] {
        var r: [(Key, Value)] = []
        for (k, v) in self {
            let tuple = [(k, v)]
            r += tuple
        }
        return r
    }
    
    ///
    /// Updates this by looking up value under ``key``, applying ``update`` to it if it exists,
    /// else setting ``key`` to be ``notFound``
    ///
    mutating func updated(key: Key, notFound: Value, update: Value -> Value) -> Void {
        if let x = self[key] {
            self[key] = update(x)
        } else {
            self[key] = notFound
        }
    }
    
    ///
    /// Updates this by looking up value under ``key`` and applying ``update`` to it.
    ///
    mutating func updated(key: Key, update: Value -> Value) -> Void {
        if let x = self[key] {
            self[key] = update(x)
        }
    }

}