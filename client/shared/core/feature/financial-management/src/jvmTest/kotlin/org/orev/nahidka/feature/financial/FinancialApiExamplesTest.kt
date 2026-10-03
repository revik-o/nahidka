package org.orev.nahidka.feature.financial

import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.NullablePatch
import org.orev.nahidka.feature.financial.command.PlanningRowInput
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.PlanningQuery
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.dto.SavingsGuidelinePolicy

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialApiExamplesTest {
    @Test
    fun documentedPartialUpdatesPreserveConflictsAndFeedMonthlySummary() = runTest {
        val fixture = FinancialTestFixture()
        val finance = exampleFinance(fixture)
        var generatedId = 0
        fun nextId(): String = "update-${++generatedId}"
        try {
            val category = fixture.category("food", "Groceries")
            val created = fixture.operation("groceries", category.id, 1_234, description = "Groceries")
            val update = UpdateFinancialOperation(
                meta = CommandMeta(nextId()),
                id = created.id,
                expectedVersion = created.version,
                patch = FinancialOperationPatch(amount = Money(fixture.usd.id, 1_500), description = NullablePatch.Clear),
            )
            val updated = committed(finance.financialManager.updateFinancialManipulation(update))
            assertEquals(null, updated.value.description)

            val stale = finance.financialManager.updateFinancialManipulation(update.copy(meta = CommandMeta(nextId())))
            val conflict = assertIs<MutationResult.Rejected>(stale)
            assertEquals(updated.value, assertIs<FinancialError.OperationConflict>(conflict.error).current)

            val snapshot = finance.financialContext.observeFinancialSnapshot(fixture.query()).first()
            assertEquals(1_500, snapshot.spending.netExpense.units)
        } finally {
            fixture.gateway.close()
        }
    }

    @Test
    fun documentedPlanningAndCategoryCommandsShareOneSession() = runTest {
        val fixture = FinancialTestFixture()
        val finance = exampleFinance(fixture)
        var generatedId = 0
        fun nextId(): String = "planning-${++generatedId}"
        var planJob: Job? = null
        try {
            val category = fixture.category("food", "Groceries")
            fixture.operation("groceries", category.id, 1_500)
            val table = FinancialPlanningTableInput(
                id = nextId(),
                month = fixture.month,
                assetId = fixture.usd.id,
                openingAvailable = Money(fixture.usd.id, 100_000),
                savingsPolicy = SavingsGuidelinePolicy(Money(fixture.usd.id, 10_000), 5_000),
                rows = persistentListOf(
                    PlanningRowInput(nextId(), category.id, Money(fixture.usd.id, 30_000), true, PaymentMethod.CARD),
                ),
            )
            val saved = committed(
                finance.financialPlanningManager.saveTable(SavePlanningTable(CommandMeta(nextId()), null, table)),
            )
            assertEquals(1_500, saved.value.rows.single().netSpent.units)

            val planningChanges = mutableListOf<PlanningTableView>()
            planJob = finance.financialPlanningContext.subscribe(PlanningQuery(fixture.month, fixture.usd.id))
                .onSnapshot { planningChanges += it.entities }
                .onInsert { planningChanges += it.after }
                .onUpdate { planningChanges += it.after }
                .launchIn(backgroundScope)
            runCurrent()
            assertEquals(saved.value.document.input.id, planningChanges.last().document.input.id)

            val renamed = committed(
                finance.financialCategoryManager.updateCategory(
                    UpdateFinancialCategory(CommandMeta(nextId()), category.id, category.version, "Food and groceries"),
                ),
            )
            val referencedDelete = finance.financialCategoryManager.deleteCategory(
                DeleteFinancialCategory(CommandMeta(nextId()), renamed.value.id, renamed.value.version),
            )
            assertIs<FinancialError.CategoryInUse>(assertIs<MutationResult.Rejected>(referencedDelete).error)
            val archived = committed(
                finance.financialCategoryManager.archiveCategory(
                    ArchiveFinancialCategory(CommandMeta(nextId()), renamed.value.id, renamed.value.version),
                ),
            )
            assertEquals(true, archived.value.archived)
        } finally {
            planJob?.cancel()
            fixture.gateway.close()
        }
    }

    private fun exampleFinance(fixture: FinancialTestFixture): FinancialModule =
        createGraphFactory<FinancialApiExampleGraph.Factory>().create(fixture.gateway).finance

    private fun <T> committed(result: MutationResult<T>): MutationResult.Committed<T> =
        assertIs<MutationResult.Committed<T>>(result)
}
