import Foundation

///
/// Use this to wrap a potentially failing asynchronous operations (taking ``Result<?> -> ()``) as their
/// callback.
///
/// Typical application is
///
/// ResultContext.run { ctx in 
///    LiftServer.sharedInstance.doSomething(params, ctx.apply { r in ... })
///    LiftServer.sharedInstance.somethingElse(ctx.apply { r in ... })
///    ...
/// }
///
/// Should any of the operations fail, a warning will be displayed, and the users will have the option of
/// retrying all the operations.
///
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
                RKDropdownAlert.title("Offline".localized(), message: "Retry?".localized(), backgroundColor: UIColor.orangeColor(), textColor: UIColor.blackColor(), time: 3)
            }
        }
    }

    ///
    /// Call this function to construct an appropriate context and kick off the operation in ``f``
    ///
    class func run(f: ResultContext -> Void) -> Void {
        let ctx = ResultContext(f)
        ctx.run()
    }
    
    ///
    /// Use this convenience function in place of ``apply { _ in }``, i.e. simply kicking off the potential side-effects
    ///
    func unit() -> (Result<()> -> Void) {
        return apply(const(()))
    }

    ///
    /// Unwrap the value in ``Result<V>``, observing the ``V`` in case of success, and tracking the failures.
    ///
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
