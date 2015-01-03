import Foundation

class ResultContext {
    var hasErrors: Bool = false
    
    class func run(f: ResultContext -> Void) -> Void {
        let ctx = ResultContext()
        f(ctx)
        if ctx.hasErrors {
            RKDropdownAlert.title("Lift server is offline", backgroundColor: UIColor.redColor(), textColor: UIColor.whiteColor(), time: 3)
        }
    }
    
    func unit() -> (Result<()> -> Void) {
        return apply(const(()))
    }

    func apply<V>(r: V -> Void) -> (Result<V> -> Void) {
        return { (x: Result<V>) in x.cata({ _ in self.hasErrors = true }, r) }
    }
    
}

