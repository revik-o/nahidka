package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList

data class PlanningTableView(
    val document: FinancialPlanningTable,
    val rows: PersistentList<PlanningRowView>,
    val totals: PlanningTotals,
)
