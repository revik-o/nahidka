package org.orev.nahidka.feature.financial.store

import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.PlanningTableView

internal sealed interface FinancialValue {
    data class Operation(val value: FinancialOperation) : FinancialValue
    data class Category(val value: FinancialCategory) : FinancialValue
    data class PlanningTable(
        val document: FinancialPlanningTable,
        val view: PlanningTableView,
    ) : FinancialValue
}
