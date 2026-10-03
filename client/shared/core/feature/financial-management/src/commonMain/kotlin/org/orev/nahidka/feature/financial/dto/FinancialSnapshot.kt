package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList

data class FinancialSnapshot(
    val sessionIdentity: String,
    val storeRevision: Long,
    val period: ReportingPeriod,
    val asset: AssetDefinition,
    val operations: PersistentList<FinancialOperation>,
    val categories: PersistentList<FinancialCategory>,
    val spending: SpendingSummary,
    val planning: PlanningState,
)
