package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.ShareDivision

fun divideShare(numerator: Long, denominator: Long): ShareDivision {
    require(denominator > 0 && numerator in 0..denominator)
    val threshold = denominator - numerator
    var remainder = 0L
    var quotient = 0
    repeat(10_000) {
        if (remainder >= threshold) {
            remainder -= threshold
            quotient++
        } else {
            remainder += numerator
        }
    }
    return ShareDivision(quotient, remainder)
}
