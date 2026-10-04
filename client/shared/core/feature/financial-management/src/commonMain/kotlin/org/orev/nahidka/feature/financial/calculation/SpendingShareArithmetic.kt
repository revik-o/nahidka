package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.ShareDivision

fun divideShare(numerator: Long, denominator: Long): ShareDivision {
    require(numerator >= 0 && denominator >= 0)

    if (denominator == 0L || numerator == 0L) {
        return ShareDivision(0, 0L)
    }

    require(numerator <= denominator)

    if (numerator <= Long.MAX_VALUE / 10_000L) {
        val scaledNumerator = numerator * 10_000L
        return ShareDivision((scaledNumerator / denominator).toInt(), scaledNumerator % denominator)
    }

    var quotient = 0
    var remainder = 0L
    var bit = 8192

    while (bit != 0) {
        quotient *= 2

        if (remainder >= denominator - remainder) {
            remainder -= denominator - remainder
            quotient++
        } else {
            remainder += remainder
        }

        if (10_000 and bit != 0) {
            if (remainder >= denominator - numerator) {
                remainder -= denominator - numerator
                quotient++
            } else {
                remainder += numerator
            }
        }

        bit = bit shr 1
    }

    return ShareDivision(quotient, remainder)
}
