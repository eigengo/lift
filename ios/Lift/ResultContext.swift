import Foundation

class ResultContext : NSObject {
    var hasErrors: Bool = false
    var callbackCounter: Int32 = 0
    var f: ResultContext -> Void
    
    internal required init(f: ResultContext -> Void) {
        self.f = f
        super.init()
    }
    
    private func run() {
        f(self)
    }
    
    private func callbackRan() {
        if OSAtomicDecrement32(&callbackCounter) == 0 {
            if hasErrors {
                RKDropdownAlert.title("Lift server is offline", backgroundColor: UIColor.redColor(), textColor: UIColor.whiteColor(), time: 3)
            }
        }
    }
    
    class func run(f: ResultContext -> Void) -> Void {
        let ctx = ResultContext(f)
        ctx.run()
    }
    
    func unit() -> (Result<()> -> Void) {
        return apply(const(()))
    }

    func apply<V>(r: V -> Void) -> (Result<V> -> Void) {
        OSAtomicIncrement32(&callbackCounter)
        return { (x: Result<V>) in x.cata(
            { err in
                self.hasErrors = true
                self.callbackRan()
            },
            {x in
                r(x)
                self.callbackRan()
            })
        }
    }
    
}
