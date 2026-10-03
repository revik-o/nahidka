package org.orev.nahidka.feature.financial.store

import org.orev.nahidka.feature.financial.dto.FinancialError

internal sealed interface FinancialReduction {
    data class Accepted(
        val data: FinancialData,
        val value: FinancialValue,
        val changed: Boolean,
    ) : FinancialReduction

    data class Rejected(val error: FinancialError) : FinancialReduction
}
