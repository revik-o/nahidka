package org.orev.nahidka.feature.financial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.PlanningRowInput
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.PlanningConfigured
import org.orev.nahidka.feature.financial.dto.PlanningNotConfigured
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.dto.SavingsGuidelinePolicy

class FinancialPlanningTest {
    @Test
    fun wholeTableProjectionMatchesTheWorkedFundsAndSavingsExample() = runTest {
        val fixture = FinancialTestFixture()
        val groceries = fixture.category("groceries", "Groceries")
        val rent = fixture.category("rent", "Rent")
        val entertainment = fixture.category("entertainment", "Entertainment")
        fixture.operation("grocery-expense", groceries.id, 18_000)
        fixture.operation("rent-expense", rent.id, 60_000)
        fixture.operation("fun-expense", entertainment.id, 5_000)
        fixture.operation("salary", null, 50_000, OperationKind.INCOME)
        val input = FinancialPlanningTableInput(
            id = "may-plan",
            month = fixture.month,
            assetId = fixture.usd.id,
            openingAvailable = Money(fixture.usd.id, 100_000),
            savingsPolicy = SavingsGuidelinePolicy(Money(fixture.usd.id, 15_000), 5_000),
            rows = persistentListOf(
                PlanningRowInput("row-groceries", groceries.id, Money(fixture.usd.id, 30_000), true, PaymentMethod.CARD),
                PlanningRowInput("row-rent", rent.id, Money(fixture.usd.id, 60_000), true, PaymentMethod.CASH),
                PlanningRowInput("row-entertainment", entertainment.id, Money(fixture.usd.id, 10_000), false, null),
            ),
        )
        val result = assertIs<MutationResult.Committed<PlanningTableView>>(
            fixture.gateway.savePlanningTable(SavePlanningTable(CommandMeta("save-may-plan"), null, input)),
        )
        val totals = result.value.totals
        assertEquals(150_000, totals.fundsAvailableThisMonth.units)
        assertEquals(83_000, totals.grossExpenses.units)
        assertEquals(67_000, totals.currentAvailable.units)
        assertEquals(90_000, totals.includedBudget.units)
        assertEquals(12_000, totals.remainingReservation.units)
        assertEquals(55_000, totals.projectedAvailableAfterPlanning.units)
        assertEquals(20_000, totals.suggestedSavings?.units)
        assertEquals(12_000, result.value.rows.first { it.input.categoryId == groceries.id }.remainingBudget.units)

        val snapshot = fixture.gateway.observeFinancialSnapshot(fixture.query()).first()
        assertIs<PlanningConfigured>(snapshot.planning)
        assertEquals(result.value.storeRevision, snapshot.storeRevision)
        fixture.gateway.close()
    }

    @Test
    fun unconfiguredFundsStayUnavailableAndRefundCreditsRemainVisible() = runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        fixture.operation("old-expense", category.id, 8_000, at = kotlin.time.Instant.parse("2024-04-28T10:30:00Z"))
        fixture.operation("current-refund", category.id, 8_000, OperationKind.REFUND, PaymentMethod.CASH, "old-expense")
        val snapshot = fixture.gateway.observeFinancialSnapshot(fixture.query()).first()
        val planning = assertIs<PlanningNotConfigured>(snapshot.planning)
        assertEquals(0, planning.income.units)
        assertEquals(-8_000, planning.netExpense.units)
        assertEquals(-8_000, snapshot.spending.netExpense.units)
        assertEquals(0, snapshot.spending.drawableTotal.units)
        assertEquals(8_000, snapshot.spending.refundCredits.units)
        assertEquals(0, snapshot.spending.slices.size)
        fixture.gateway.close()
    }

    @Test
    fun donutSharesUseNetPositiveCategoriesAndRoundToExactlyTenThousandBasisPoints() = runTest {
        val fixture = FinancialTestFixture()
        val categories = (1..9).map { fixture.category("cat-$it", "Category $it") }
        categories.forEachIndexed { index, category -> fixture.operation("op-${index + 1}", category.id, index.toLong() + 1L) }
        val snapshot = fixture.gateway.observeFinancialSnapshot(fixture.query()).first()
        assertEquals(8, snapshot.spending.slices.size)
        assertEquals("projection:other", snapshot.spending.slices.last().categoryId)
        assertEquals(10_000, snapshot.spending.slices.sumOf { it.percentageBasisPoints })
        assertNull(snapshot.spending.slices.firstOrNull { it.percentageBasisPoints < 0 })
        fixture.gateway.close()
    }

    @Test
    fun operationAndPlanningProjectionsPublishOneSharedRevision() = runTest {
        val fixture = FinancialTestFixture()
        val food = fixture.category()
        val expense = fixture.operation("projected-expense", food.id, 10_000)
        val tableInput = FinancialPlanningTableInput(
            id = "projection-plan",
            month = fixture.month,
            assetId = fixture.usd.id,
            openingAvailable = Money(fixture.usd.id, 50_000),
            savingsPolicy = null,
            rows = persistentListOf(PlanningRowInput("projection-row", food.id, Money(fixture.usd.id, 20_000), true, PaymentMethod.CARD)),
        )
        assertIs<MutationResult.Committed<PlanningTableView>>(
            fixture.gateway.savePlanningTable(SavePlanningTable(CommandMeta("projection-plan-save"), null, tableInput)),
        )
        val updated = assertIs<MutationResult.Committed<FinancialOperation>>(
            fixture.gateway.updateOperation(
                UpdateFinancialOperation(
                    CommandMeta("projection-expense-update"),
                    expense.id,
                    expense.version,
                    FinancialOperationPatch(amount = Money(fixture.usd.id, 15_000)),
                ),
            ),
        )
        val snapshot = fixture.gateway.observeFinancialSnapshot(fixture.query()).first()
        val configured = assertIs<PlanningConfigured>(snapshot.planning)
        assertEquals(updated.storeRevision, snapshot.storeRevision)
        assertEquals(snapshot.storeRevision, configured.table.storeRevision)
        assertEquals(15_000, snapshot.spending.netExpense.units)
        assertEquals(15_000, configured.table.rows.single().netSpent.units)
        fixture.gateway.close()
    }

    @Test
    fun deeplyNegativeAvailableCashDoesNotOverflowWhenApplyingReserve() = runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        val input = FinancialPlanningTableInput(
            id = "negative-plan",
            month = fixture.month,
            assetId = fixture.usd.id,
            openingAvailable = Money(fixture.usd.id, Long.MIN_VALUE + 10),
            savingsPolicy = SavingsGuidelinePolicy(Money(fixture.usd.id, 100), 5_000),
            rows = persistentListOf(PlanningRowInput("negative-row", category.id, Money(fixture.usd.id, 0), true, null)),
        )
        val saved = assertIs<MutationResult.Committed<PlanningTableView>>(
            fixture.gateway.savePlanningTable(SavePlanningTable(CommandMeta("save-negative-plan"), null, input)),
        )
        assertEquals(Long.MIN_VALUE + 10, saved.value.totals.projectedAvailableAfterPlanning.units)
        assertEquals(0, saved.value.totals.suggestedSavings?.units)
        fixture.gateway.close()
    }
}
