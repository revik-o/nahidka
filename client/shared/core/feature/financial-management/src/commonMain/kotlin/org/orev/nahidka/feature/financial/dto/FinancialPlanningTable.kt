package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList
import kotlinx.datetime.YearMonth

data class FinancialPlanningTable(
    val version: Long,
    val identifier: String,
    val month: YearMonth,
    val assetIdentifier: String,
    val openingAvailable: Money,
    val savingsPolicy: SavingsGuidelinePolicy?,
    val rows: PersistentList<PlanningRow>,
)
