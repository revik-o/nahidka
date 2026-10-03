package org.orev.nahidka.ui.dashboard

import kotlinx.collections.immutable.PersistentList
import org.orev.nahidka.feature.financial.dto.SpendingSummary

data class FinancialOverviewUi(
    val formattedSpent: String,
    val formattedIncome: String,
    val formattedAvailableAfterPlanning: String?,
    val spending: SpendingSummary,
    val assetDisplayCode: String,
    val spendingSlices: PersistentList<FinancialSpendingSliceUi>,
    val formattedRefundCredits: String,
    val planningConfigured: Boolean,
)
