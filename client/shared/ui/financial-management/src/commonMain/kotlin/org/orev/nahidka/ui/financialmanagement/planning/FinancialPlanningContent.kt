package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.collections.immutable.PersistentList
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable

data class FinancialPlanningContent(
    val month: YearMonth,
    val asset: AssetDefinition,
    val planningTable: FinancialPlanningTable?,
    val summary: FinancialPlanningSummary,
    val rows: PersistentList<FinancialPlanningRow>,
    val categories: PersistentList<FinancialCategory>,
)
