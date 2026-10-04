package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FINANCIAL_COMPACT_LAYOUT_WIDTH_LIMIT = 600.dp

internal enum class FinancialLayoutWidth {
    COMPACT,
    EXPANDED;

    companion object {

        fun of(availableWidth: Dp): FinancialLayoutWidth =
            if (availableWidth < FINANCIAL_COMPACT_LAYOUT_WIDTH_LIMIT) COMPACT else EXPANDED
    }
}
