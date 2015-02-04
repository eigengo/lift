import Foundation

class PagingGenerator<T> : GeneratorType {
    
    let original: [T]
    let pageSize: Int
    var currentPage = 0
    
    init(original: [T], pageSize: Int) {
        self.original = original
        self.pageSize = pageSize
    }
    
    func next() -> Slice<T>? {
        let lower = currentPage * pageSize
        let upper = min(lower + pageSize - 1, original.count - 1)
        
        if lower >= upper { return .None }
        
        ++currentPage
        return original[lower...upper]
    }
    
}

class PagingSequence<T> : SequenceType {
    
    let original: [T]
    let pageSize: Int
    
    init(original: [T], pageSize: Int) {
        self.original = original
        self.pageSize = pageSize
    }
    
    func generate() -> PagingGenerator<T> {
        return PagingGenerator(original: original, pageSize: pageSize)
    }
    
}

extension Array {

    func pages(size: Int) -> PagingSequence<T> {
        return PagingSequence(original: self, pageSize: size)
    }
    
}