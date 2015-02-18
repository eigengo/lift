import Foundation

public class LiftServerCache {
 
    ///
    /// Singleton instance of the LiftServer. The instances are stateless, so it is generally a
    /// good idea to take advantage of the singleton
    ///
    public class var sharedInstance: LiftServerCache {
        struct Singleton {
            static let instance = LiftServerCache()
        }
        
        return Singleton.instance
    }
    
    private var muscleGroups: [Exercise.MuscleGroup]?
    
    func build(f: Result<Void> -> Void) -> Void {
        LiftServer.sharedInstance.exerciseGetMuscleGroups() {
            $0.cata(const(()), { x in self.muscleGroups = x })
            f(Result.value(()))
        }
    }
    
    func clean() {
        self.muscleGroups = nil
    }
    
    func exerciseGetMuscleGroups() -> [Exercise.MuscleGroup]? {
        return self.muscleGroups
    }
}