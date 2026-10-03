package org.orev.nahidka.feature.financial.dto

import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput

data class FinancialPlanningTable(
    val version: Long,
    val input: FinancialPlanningTableInput,
)
