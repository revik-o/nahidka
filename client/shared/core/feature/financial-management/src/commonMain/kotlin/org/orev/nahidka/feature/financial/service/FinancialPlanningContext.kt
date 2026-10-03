package org.orev.nahidka.feature.financial.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.PlanningQuery
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription

@SingleIn(FinancialSessionScope::class)
class FinancialPlanningContext @Inject constructor(private val gateway: FinancialGateway) {
    fun subscribe(query: PlanningQuery = PlanningQuery()): FinancialSubscription<PlanningTableView> =
        gateway.subscribePlanning(query)
}
