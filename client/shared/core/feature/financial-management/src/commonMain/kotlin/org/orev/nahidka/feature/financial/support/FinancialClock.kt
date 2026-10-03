package org.orev.nahidka.feature.financial.support

import kotlin.time.Instant

fun interface FinancialClock {
    fun now(): Instant
}
