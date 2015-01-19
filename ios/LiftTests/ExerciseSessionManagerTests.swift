import Foundation
import XCTest

class ExerciseSessionManagerTests : XCTestCase {
    let manager = ExerciseSessionManager.sharedInstance
    let defaultMultiPacket: MultiPacket = MutableMultiPacket().appendPayload(SensorDataSourceLocation.Wrist, payload: 0xff, 0x01, 0x02, 0x03)
    
    override func setUp() {
        manager.removeAllOfflineSessions()
        XCTAssert(manager.listOfflineSessions(NSDate()).isEmpty, "Existing offline sessions found")
    }
    
    private func assertSessionFound(props: Exercise.SessionProps) -> Void {
        let os = manager.listOfflineSessions(NSDate()).first!
        XCTAssert(os.muscleGroupKeys == props.muscleGroupKeys, "Expected session not found")
    }
    
    func testOfflineFromStart() {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        let s = manager.managedSession(ExerciseSession(id: NSUUID(), props: props), isOfflineFromStart: true)
        
        // even before submitting, we must see this offline session
        assertSessionFound(props)
        
        // three lots of file submissions later...
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }

        // the session should still be there
        assertSessionFound(props)
        
        // end it
        s.end(const(()))
        
        // the session should still be there
        assertSessionFound(props)
    }
    
    func testOnlineStartThenImmediatelyOffline() {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        let s = manager.managedSession(ExerciseSession(id: NSUUID(), props: props), isOfflineFromStart: false)
        
        // even before submitting, we must see this offline session
        assertSessionFound(props)
        
        // the first submission fails—no server
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        
        // end it
        s.end(const(()))
        
        // the session should still be there
        assertSessionFound(props)
    }
    
    func testFailureHalfWay() {
        let server = HTTPServer()
        server.setPort(12345)
        server.start(nil)
        server.setConnectionClass(RunningSessionHttpConnection)
        
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        let s = manager.managedSession(ExerciseSession(id: NSUUID(), props: props), isOfflineFromStart: false)
        
        // even before submitting, we must see this offline session
        assertSessionFound(props)
        
        // the first submission fails—no server
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        
        // end it
        s.end(const(()))
        
        // the session should still be there
        assertSessionFound(props)
    }

    class RunningSessionHttpConnection : HTTPConnection {
        override func httpResponseForMethod(method: String!, URI path: String!) -> NSObject {
            return HTTPDataResponse(data: NSData())
        }
    }
}
