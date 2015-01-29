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
        //  ^        ^ Exact match (0..10, 1 B per sample, 1 s/s)
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 10), maximumGap: 0, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), data.asString())

        // [01234|56789]
        //  ^        ^ Exact match (0..2,  5 B per sample, 1 s/s)
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 2), maximumGap: 0, sampleSize: 5, samplesPerSecond: 1, gapValue: dash)!.asString(), data.asString())

        // [01234|56789]
        //  ^        ^ Exact match (0..1,  5 B per sample, 2 s/s)
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 1), maximumGap: 0, sampleSize: 5, samplesPerSecond: 2, gapValue: dash)!.asString(), data.asString())
    }
    
    func testSliceWithoutGaps() {
        // [0123456789]
        //   ^      ^ Within our data
        let w1 = data.slice(TimeRange(start: 1, end: 9), maximumGap: 0, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(w1.asString(), "12345678")
        XCTAssertEqual(w1.startTime, 1)
        XCTAssertEqual(w1.duration(1, samplesPerSecond: 1), 8)
        
        // [0123456789]
        //  ^    ^ Within our data
        XCTAssertEqual(data.slice(TimeRange(start: 0, end: 6), maximumGap: 0, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!.asString(), "012345")
    }
    
    func testSliceWithinMaximumGaps() {
        // [-0123456789]
        //  ^         ^ Gap at start
        let gas = data.slice(TimeRange(start: -1, end: 10), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(gas.asString(), "-0123456789")
        XCTAssertEqual(gas.startTime, -1)
        XCTAssertEqual(gas.duration(1, samplesPerSecond: 1), 11)

        // [0123456789-]
        //  ^         ^ Gap at end
        let gae = data.slice(TimeRange(start: 0, end: 11), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(gae.asString(), "0123456789-")
        XCTAssertEqual(gae.startTime, 0)
        XCTAssertEqual(gae.duration(1, samplesPerSecond: 1), 11)
        
        // [-0123456789-]
        //  ^          ^ Gap at start and at end
        let gse = data.slice(TimeRange(start: -1, end: 11), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(gse.asString(), "-0123456789-")
        XCTAssertEqual(gse.startTime, -1)
        XCTAssertEqual(gse.duration(1, samplesPerSecond: 1), 12)

        // [-0123456789]
        //  ^     ^ Gap at start, trim at end
        let gste = data.slice(TimeRange(start: -1, end: 6), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(gste.asString(), "-012345")
        XCTAssertEqual(gste.startTime, -1)
        XCTAssertEqual(gste.duration(1, samplesPerSecond: 1), 7)

        // [0123456789-]
        //     ^      ^ Trim at start, gap at end
        let tsge = data.slice(TimeRange(start: 3, end: 11), maximumGap: 1, sampleSize: 1, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(tsge.asString(), "3456789-")
        XCTAssertEqual(tsge.startTime, 3)
        XCTAssertEqual(tsge.duration(1, samplesPerSecond: 1), 8)
    }

    func testSliceWithinMaximumGapsSampleSize5() {
        // [-----|01234|56789]
        //  ^               ^ Gap at start
        let gas = data.slice(TimeRange(start: -1, end: 2), maximumGap: 1, sampleSize: 5, samplesPerSecond: 1, gapValue: dash)!
        XCTAssertEqual(gas.asString(), "-----0123456789")
        XCTAssertEqual(gas.startTime, -1)
        XCTAssertEqual(gas.duration(5, samplesPerSecond: 1), 3)
    }
}
