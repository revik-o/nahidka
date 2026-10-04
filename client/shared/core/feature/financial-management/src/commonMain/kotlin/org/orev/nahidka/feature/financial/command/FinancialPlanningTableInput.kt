package org.orev.nahidka.feature.financial.command

import kotlinx.collections.immutable.PersistentList
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.SavingsGuidelinePolicy

data class FinancialPlanningTableInput(
    val identifier: String,
    val month: YearMonth,
    val assetIdentifier: String,
    val openingAvailable: Money,
    val savingsPolicy: SavingsGuidelinePolicy?,
    val rows: PersistentList<PlanningRowInput>,
)
