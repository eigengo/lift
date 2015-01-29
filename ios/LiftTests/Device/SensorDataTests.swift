import Foundation
import XCTest

class SensorDataTests : XCTestCase {
    let data = SensorData.fromString("0123456789", startingAt: 0)
    let dash: UInt8 = 0x2d  // '-'
    
    func testDuration() {
        XCTAssertEqual(data.duration(1, samplesPerSecond: 1), 10)
        XCTAssertEqual(data.duration(1, samplesPerSecond: 10), 1)
        XCTAssertEqual(data.duration(10, samplesPerSecond: 1), 1)
    }
    
    func testSliceOverMaximumGap() {
        // [-0123456789]
        // ^           ^ Over gap at start
        XCTAssertTrue(data.slice(TimeRange(start: -2, end: 10), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash) == nil)
        // [0123456789-]
        //  ^          ^ Over gap at end
        XCTAssertTrue(data.slice(TimeRange(start: 0, end: 12), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash) == nil)
    }
    
    func testSliceExact() {
        // [0123456789]
        //  ^        ^ Exact match
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 10), maximumGap: 0, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), data.asString())
    }
    
    func testSliceWithoutGaps() {
        // [0123456789]
        //   ^      ^ Within our data
        XCTAssertEqual(data.slice(TimeRange(start: 1, end: 9), maximumGap: 0, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "12345678")
        
        // [0123456789]
        //  ^    ^ Within our data
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 6), maximumGap: 0, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "012345")
    }
    
    func testSliceWithinMaximumGaps() {
        // [-0123456789]
        //  ^         ^ Gap at start
        XCTAssertEqual(data.slice(TimeRange(start: -1, end: 10), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "-0123456789")

        // [0123456789-]
        //  ^         ^ Gap at end
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 11), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "0123456789-")
        
        // [-0123456789-]
        //  ^          ^ Gap at start and at end
        XCTAssertEqual(data.slice(TimeRange(start: -1, end: 11), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "-0123456789-")

        // [-0123456789]
        //  ^     ^ Gap at start, trim at end
        XCTAssertEqual(data.slice(TimeRange(start: -1, end: 6), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "-012345")

        // [0123456789-]
        //     ^      ^ Trim at start, gap at end
        XCTAssertEqual(data.slice(TimeRange(start: 3, end: 11), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "3456789-")
    }

}
