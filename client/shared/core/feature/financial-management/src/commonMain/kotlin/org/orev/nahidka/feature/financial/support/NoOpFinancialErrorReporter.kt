package org.orev.nahidka.feature.financial.support

import dev.zacsweers.metro.Inject

class NoOpFinancialErrorReporter @Inject constructor() : FinancialErrorReporter {
    override fun report(failure: Throwable) = Unit
}
