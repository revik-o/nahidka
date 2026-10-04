package org.orev.nahidka.feature.financial.command

import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable

fun FinancialPlanningTableInput.toPlanningTable(version: Long): FinancialPlanningTable = FinancialPlanningTable(
    version,
    identifier,
    month,
    assetIdentifier,
    openingAvailable,
    savingsPolicy,
    rows,
)

fun FinancialPlanningTable.toInput(): FinancialPlanningTableInput = FinancialPlanningTableInput(
    identifier,
    month,
    assetIdentifier,
    openingAvailable,
    savingsPolicy,
    rows,
)
