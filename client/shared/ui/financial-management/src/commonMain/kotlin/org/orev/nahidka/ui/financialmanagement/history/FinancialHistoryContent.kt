package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.collections.immutable.PersistentList
import org.orev.nahidka.feature.financial.dto.FinancialCategory

data class FinancialHistoryContent(
    val rows: PersistentList<FinancialOperationRow>,
    val hasMoreRows: Boolean,
    val categories: PersistentList<FinancialCategory>,
)
