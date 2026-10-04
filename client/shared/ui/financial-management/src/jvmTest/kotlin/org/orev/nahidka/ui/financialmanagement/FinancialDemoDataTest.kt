package org.orev.nahidka.ui.financialmanagement

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.core.common.NoOpErrorReporter
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

internal class FinancialDemoDataTest {

    @Test
    fun demoDataProvidesCategoriesPagedHistoryAndPlanningForBothAssets() = runTest {
        val gateway = InMemoryFinancialGateway(FinancialDemoData.sessionConfig, NoOpErrorReporter(), FinancialCalendar())
        val clock = ApplicationClock { Instant.parse("2026-10-05T12:00:00Z") }
        try {
            FinancialDemoData.populate(gateway, clock)
            FinancialDemoData.populate(gateway, clock)

            FinancialDemoData.sessionConfig.assets.forEach { asset ->
                val snapshot = gateway.observeFinancialSnapshot(MonthlyQuery(YearMonth(2026, 10), asset.identifier)).first()
                val multiplier = if (asset.displayCode == "UAH") 40L else 1L
                assertEquals(5, snapshot.categories.size)
                assertEquals(56, snapshot.operations.size)
                assertTrue(snapshot.operations.all { operation -> operation.amount.assetIdentifier == asset.identifier })
                assertEquals(PaymentMethod.entries.toSet(), snapshot.operations.map(FinancialOperation::paymentMethod).toSet())
                val plan = (snapshot.planning as PlanningConfigured).table
                assertEquals(3, plan.rows.size)
                assertEquals(300_000L * multiplier, plan.totals.fundsAvailableThisMonth.units)
                assertEquals(235_375L * multiplier, plan.totals.currentAvailable.units)
                val emptyMonth = gateway.observeFinancialSnapshot(MonthlyQuery(YearMonth(2026, 11), asset.identifier)).first()
                assertTrue(emptyMonth.operations.isEmpty())
                assertEquals(0, (emptyMonth.planning as PlanningNotConfigured).currentAvailable.units)
            }
        } finally {
            gateway.close()
        }
    }
}
