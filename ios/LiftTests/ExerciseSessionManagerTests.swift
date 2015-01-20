import Foundation
import XCTest

class ExerciseSessionManagerTests : XCTestCase {
    let manager = ExerciseSessionManager.sharedInstance
    let defaultMultiPacket: MultiPacket = MutableMultiPacket().appendPayload(SensorDataSourceLocation.Wrist, payload: 0xff, 0x01, 0x02, 0x03)
    
    override func setUp() {
        LiftServer.sharedInstance.setBaseUrlString("http://localhost:12345")
        manager.removeAllOfflineSessions()
        XCTAssert(manager.listOfflineSessions().isEmpty, "Existing offline sessions found")
    }
    
    private func findSessionWithProps(props: Exercise.SessionProps) -> Bool {
        let c = manager.listOfflineSessions().filter { x in
                x.props.muscleGroupKeys == props.muscleGroupKeys &&
                abs(x.props.startDate.timeIntervalSinceDate(props.startDate)) < 0.01 &&
                x.props.intendedIntensity == props.intendedIntensity
        }.count
        return c == 1
    }
    
    func testOfflineFromStart() {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        let s = manager.managedSession(ExerciseSession(id: NSUUID(), props: props), isOfflineFromStart: true)
        
        // even before submitting, we must see this offline session
        XCTAssert(findSessionWithProps(props), "Expected session not found")
        
        // three lots of file submissions later...
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }

        // the session should still be there
        XCTAssert(findSessionWithProps(props), "Expected session not found")
        
        // end it
        s.end(const(()))
        
        // the session should still be there
        XCTAssert(findSessionWithProps(props), "Expected session not found")
    }
    
    func testOnlineStartThenImmediatelyOffline() {
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        let s = manager.managedSession(ExerciseSession(id: NSUUID(), props: props), isOfflineFromStart: false)
        
        // even before submitting, we must see this offline session
        XCTAssert(findSessionWithProps(props), "Expected session not found")
        
        // the first submission fails—no server
        s.submitData(defaultMultiPacket) { $0.cata(const(()), { _ in XCTFail("First failing request must not fail")}) }
        // we're now offline, and the next submit must succeed
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        
        // end it
        s.end(const(()))
        
        // the session should still be there
        XCTAssert(findSessionWithProps(props), "Expected session not found")
    }
    
    func testFailureHalfWay() {
        let server = HTTPServer()
        server.setPort(12345)
        server.setConnectionClass(RunningSessionHttpConnection)
        server.start(nil)
        
        let props = Exercise.SessionProps(startDate: NSDate(), muscleGroupKeys: ["arms"], intendedIntensity: 1.0)
        let s = manager.managedSession(ExerciseSession(id: NSUUID(), props: props), isOfflineFromStart: false)
        
        // even before submitting, we must see this offline session
        XCTAssert(findSessionWithProps(props), "Expected session not found")
        
        // the first submission works
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        
        // the next submission must fail
        s.submitData(defaultMultiPacket) { $0.cata(const(()), { _ in XCTFail("Online must fail")}) }
        server.stop(false)
        
        // we're now offline, and the next submit must succeed
        s.submitData(defaultMultiPacket) { $0.cata({ _ in XCTFail("Offline must not fail")}, const(())) }
        
        // end it
        s.end(const(()))
        
        // the session should still be there
        XCTAssert(findSessionWithProps(props), "Expected session not found")
    }
    
}

class RunningSessionHttpConnection : HTTPConnection {
    override func httpResponseForMethod(method: String!, URI path: String!) -> NSObject {
        return HTTPDataResponse(data: NSData())
    }
}
