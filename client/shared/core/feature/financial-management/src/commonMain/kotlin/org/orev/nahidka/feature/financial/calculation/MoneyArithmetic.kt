package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.core.common.ArithmeticOverflowException
import org.orev.nahidka.feature.financial.dto.Money

fun checkedAdd(left: Long, right: Long): Long {
    if (right > 0 && left > Long.MAX_VALUE - right) {
        throw ArithmeticOverflowException()
    }

    if (right < 0 && left < Long.MIN_VALUE - right) {
        throw ArithmeticOverflowException()
    }

    return left + right
}

fun checkedSubtract(left: Long, right: Long): Long {
    if (right > 0 && left < Long.MIN_VALUE + right) {
        throw ArithmeticOverflowException()
    }

    if (right < 0 && left > Long.MAX_VALUE + right) {
        throw ArithmeticOverflowException()
    }

    return left - right
}

fun allocateBasisPoints(units: Long, rate: Int): Long {
    require(units >= 0 && rate in 0..10_000)

    val whole = (units / 10_000) * rate.toLong()
    val remainder = ((units % 10_000) * rate.toLong()) / 10_000

    return checkedAdd(whole, remainder)
}

fun checkedMoneyAdd(left: Money, right: Money): Money {
    require(left.assetIdentifier == right.assetIdentifier) {
        "Money assets do not match"
    }

    return Money(left.assetIdentifier, checkedAdd(left.units, right.units))
}

fun checkedMoneySubtract(left: Money, right: Money): Money {
    require(left.assetIdentifier == right.assetIdentifier) {
        "Money assets do not match"
    }

    return Money(left.assetIdentifier, checkedSubtract(left.units, right.units))
}
