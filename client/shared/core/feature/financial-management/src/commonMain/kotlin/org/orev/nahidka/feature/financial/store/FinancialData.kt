package org.orev.nahidka.feature.financial.store

import kotlinx.collections.immutable.PersistentMap
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable

internal data class FinancialData(
    val operations: PersistentMap<String, FinancialOperation>,
    val categories: PersistentMap<String, FinancialCategory>,
    val planningTables: PersistentMap<String, FinancialPlanningTable>,
)
