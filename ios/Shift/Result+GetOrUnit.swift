import Foundation

extension Result {
    
    func getOrUnit(r: V -> Void) -> Void {
        cata(const(()), r: r)
    }
    
}