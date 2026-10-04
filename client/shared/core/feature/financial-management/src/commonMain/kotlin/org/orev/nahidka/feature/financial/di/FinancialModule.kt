package org.orev.nahidka.feature.financial.di

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

@SingleIn(FinancialSessionScope::class)
class FinancialModule @Inject constructor(val financialGateway: FinancialGateway)
