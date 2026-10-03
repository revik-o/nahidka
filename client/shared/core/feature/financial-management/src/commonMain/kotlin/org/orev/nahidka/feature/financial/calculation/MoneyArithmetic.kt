package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.support.FinancialOverflowException

fun checkedAdd(left: Long, right: Long): Long {
    if (right > 0 && left > Long.MAX_VALUE - right) throw FinancialOverflowException()
    if (right < 0 && left < Long.MIN_VALUE - right) throw FinancialOverflowException()
    return left + right
}

fun checkedSubtract(left: Long, right: Long): Long {
    if (right > 0 && left < Long.MIN_VALUE + right) throw FinancialOverflowException()
    if (right < 0 && left > Long.MAX_VALUE + right) throw FinancialOverflowException()
    return left - right
}

fun checkedNextVersion(version: Long): Long {
    if (version < 0 || version >= Long.MAX_VALUE) throw FinancialOverflowException()
    return version + 1
}

fun allocateBasisPoints(units: Long, rate: Int): Long {
    require(units >= 0 && rate in 0..10_000)
    val whole = (units / 10_000) * rate.toLong()
    val remainder = ((units % 10_000) * rate.toLong()) / 10_000
    return checkedAdd(whole, remainder)
}

fun checkedMoneyAdd(left: Money, right: Money): Money {
    require(left.assetId == right.assetId) { "Money assets do not match" }
    return Money(left.assetId, checkedAdd(left.units, right.units))
}

fun checkedMoneySubtract(left: Money, right: Money): Money {
    require(left.assetId == right.assetId) { "Money assets do not match" }
    return Money(left.assetId, checkedSubtract(left.units, right.units))
}
