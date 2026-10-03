package org.orev.nahidka.feature.financial.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.CategoryQuery
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription

@SingleIn(FinancialSessionScope::class)
class FinancialCategoryContext @Inject constructor(private val gateway: FinancialGateway) {
    fun subscribe(query: CategoryQuery = CategoryQuery()): FinancialSubscription<FinancialCategory> =
        gateway.subscribeCategories(query)
}
