package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList

data class SpendingSummary(
    val storeRevision: Long,
    val period: ReportingPeriod,
    val assetId: String,
    val grossExpense: Money,
    val refunds: Money,
    val netExpense: Money,
    val drawableTotal: Money,
    val refundCredits: Money,
    val slices: PersistentList<SpendingSlice>,
)
