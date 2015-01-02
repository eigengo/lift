import Foundation

extension Result {
 
    func ddv(f: V -> Void) -> Void {
        fold((), f)
    }
    
}