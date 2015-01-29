import Foundation
import XCTest

func XCTAssertArraysEqual<A : Equatable>(lhs: @autoclosure () -> [A], rhs: @autoclosure () -> [A]) {
    if lhs().count != rhs().count { XCTFail("Arrays not equal (counts)") }
    for (i, lsd) in enumerate(lhs()) {
        let rsd = rhs()[i]
        if lsd != rsd { XCTFail("Arrays not equal elements at index \(i)") }
    }
}
