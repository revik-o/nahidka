package org.orev.nahidka.feature.financial.di

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.service.FinancialCategoryContext
import org.orev.nahidka.feature.financial.service.FinancialCategoryService
import org.orev.nahidka.feature.financial.service.FinancialContext
import org.orev.nahidka.feature.financial.service.FinancialPlanningContext
import org.orev.nahidka.feature.financial.service.FinancialPlanningService
import org.orev.nahidka.feature.financial.service.FinancialService

@SingleIn(FinancialSessionScope::class)
class FinancialModule @Inject constructor(
    val financialManager: FinancialService,
    val financialContext: FinancialContext,
    val financialPlanningManager: FinancialPlanningService,
    val financialPlanningContext: FinancialPlanningContext,
    val financialCategoryManager: FinancialCategoryService,
    val financialCategoryContext: FinancialCategoryContext,
)
