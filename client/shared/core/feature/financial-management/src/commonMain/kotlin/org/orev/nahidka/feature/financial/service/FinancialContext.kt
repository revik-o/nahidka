package org.orev.nahidka.feature.financial.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialSnapshot
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.dto.SpendingSummary
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription

@SingleIn(FinancialSessionScope::class)
class FinancialContext @Inject constructor(private val gateway: FinancialGateway) {
    fun subscribe(query: OperationQuery = OperationQuery()): FinancialSubscription<FinancialOperation> =
        gateway.subscribeOperations(query)

    fun observeFinancialSnapshot(query: MonthlyQuery): Flow<FinancialSnapshot> =
        gateway.observeFinancialSnapshot(query)

    fun observeSpendingSummary(query: MonthlyQuery): Flow<SpendingSummary> =
        observeFinancialSnapshot(query).map { it.spending }
}
