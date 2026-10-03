package org.orev.nahidka.feature.financial.support

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Instant

class SystemFinancialClock @Inject constructor() : FinancialClock {
    override fun now(): Instant = Clock.System.now()
}
