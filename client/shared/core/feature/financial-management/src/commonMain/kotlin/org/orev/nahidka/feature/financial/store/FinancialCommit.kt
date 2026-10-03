package org.orev.nahidka.feature.financial.store

import kotlinx.collections.immutable.PersistentList
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.subscription.EntityDelta

internal data class FinancialCommit(
    val revision: Long,
    val commandId: String,
    val before: FinancialData,
    val after: FinancialData,
    val operationDeltas: PersistentList<EntityDelta<FinancialOperation>>,
    val categoryDeltas: PersistentList<EntityDelta<FinancialCategory>>,
    val planningDeltas: PersistentList<EntityDelta<FinancialPlanningTable>>,
)
