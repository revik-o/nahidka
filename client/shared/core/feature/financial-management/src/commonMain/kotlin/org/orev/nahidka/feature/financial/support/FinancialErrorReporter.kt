package org.orev.nahidka.feature.financial.support

fun interface FinancialErrorReporter {
    fun report(failure: Throwable)
}
