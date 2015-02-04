import Foundation
import XCTest

class SequenceExtensionsTest : XCTestCase {
    
    func testPagesOnEmpty() {
        let emptyArray = Array<Int>()
        for _ in emptyArray.pages(12) {
            XCTFail("Pages yields results on empty Array!")
        }
    }
    
    func testPages() {
        var i = 0
        for s in [1, 2, 3, 4, 5].pages(3) {
            if i == 0 {
                XCTAssertEqual(s.count, 3)
                XCTAssertArraysEqual([s[0], s[1], s[2]], [1, 2, 3])
                ++i
            } else if i == 1 {
                XCTAssertEqual(s.count, 2)
                XCTAssertArraysEqual([s[0], s[1]], [4, 5])
                ++i
            } else {
                XCTFail("Too many pages generated!")
            }
        }
    }
    
    func testPagesOnExactPageSize() {
        var i = 0
        for s in [1, 2, 3, 4, 5, 6].pages(3) {
            if i == 0 {
                XCTAssertEqual(s.count, 3)
                XCTAssertArraysEqual([s[0], s[1], s[2]], [1, 2, 3])
                ++i
            } else if i == 1 {
                XCTAssertEqual(s.count, 3)
                XCTAssertArraysEqual([s[0], s[1], s[2]], [4, 5, 6])
                ++i
            } else {
                XCTFail("Too many pages generated!")
            }
        }
    }
    
}