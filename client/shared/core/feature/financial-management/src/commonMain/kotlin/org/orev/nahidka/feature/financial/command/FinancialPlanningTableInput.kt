package org.orev.nahidka.feature.financial.command

import kotlinx.collections.immutable.PersistentList
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.SavingsGuidelinePolicy

data class FinancialPlanningTableInput(
    val id: String,
    val month: YearMonth,
    val assetId: String,
    val openingAvailable: Money,
    val savingsPolicy: SavingsGuidelinePolicy?,
    val rows: PersistentList<PlanningRowInput>,
)
