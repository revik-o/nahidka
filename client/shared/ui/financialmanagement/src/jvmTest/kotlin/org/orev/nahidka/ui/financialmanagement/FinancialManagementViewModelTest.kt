package org.orev.nahidka.ui.financialmanagement

import androidx.lifecycle.ViewModelStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.command.NullablePatch
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.feature.financial.service.FinancialCategoryContext
import org.orev.nahidka.feature.financial.service.FinancialContext
import org.orev.nahidka.feature.financial.service.FinancialPlanningContext
import org.orev.nahidka.feature.financial.service.FinancialPlanningService
import org.orev.nahidka.feature.financial.service.FinancialService
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.feature.financial.support.FinancialClock
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialManagementViewModelTest {
    @Test
    fun operationDraftKeepsExactInputOnValidationAndSupportsAddAndClear() = runTest {
        withFinance { viewModel, gateway, category ->
            viewModel.handleEvent(FinancialManagementEvent.OpenAddOperation)
            var draft = assertNotNull(viewModel.state.value.operationDraft)
            viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(amountText = "1.239")))
            viewModel.handleEvent(FinancialManagementEvent.SaveOperation)
            assertNotNull(viewModel.state.value.operationDraft)
            assertTrue(viewModel.state.value.feedback.orEmpty().contains("positive amount"))

            draft = viewModel.state.value.operationDraft!!
            viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(amountText = "12.34", descriptionText = "Lunch")))
            viewModel.handleEvent(FinancialManagementEvent.SaveOperation)
            runCurrent()
            val added = gateway.observeFinancialSnapshot(org.orev.nahidka.feature.financial.dto.MonthlyQuery(YearMonth(2024, 5), "iso4217:USD")).first()
                .operations.single()
            assertEquals(1_234, added.amount.units)
            assertEquals("Lunch", added.description)

            viewModel.handleEvent(FinancialManagementEvent.EditOperation(added.id))
            val edit = viewModel.state.value.operationDraft!!
            viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(edit.copy(descriptionText = "")))
            viewModel.handleEvent(FinancialManagementEvent.SaveOperation)
            runCurrent()
            val updated = gateway.observeFinancialSnapshot(org.orev.nahidka.feature.financial.dto.MonthlyQuery(YearMonth(2024, 5), "iso4217:USD")).first()
                .operations.single()
            assertNull(updated.description)
            assertEquals(2L, updated.version)
            assertEquals(category.id, updated.categoryId)
        }
    }

    @Test
    fun operationConflictPreservesDraftAndReviewedRetryUsesCurrentVersion() = runTest {
        withFinance(
            seed = { gateway, category, instant ->
                gateway.addOperation(
                    AddFinancialOperation(
                        CommandMeta("seed-operation"),
                        NewFinancialOperation("seed", Money("iso4217:USD", 1_000), OperationKind.EXPENSE, category.id, PaymentMethod.CARD, instant, "Original"),
                    ),
                )
            },
        ) { viewModel, gateway, _ ->
            runCurrent()
            viewModel.handleEvent(FinancialManagementEvent.EditOperation("seed"))
            val draft = viewModel.state.value.operationDraft!!
            viewModel.handleEvent(FinancialManagementEvent.UpdateOperationDraft(draft.copy(amountText = "20.00", descriptionText = "My reviewed change")))
            val remote = assertIs<MutationResult.Committed<org.orev.nahidka.feature.financial.dto.FinancialOperation>>(
                gateway.updateOperation(
                    UpdateFinancialOperation(
                        CommandMeta("remote-edit"),
                        "seed",
                        1,
                        FinancialOperationPatch(description = NullablePatch.Set("Remote change")),
                    ),
                ),
            ).value
            runCurrent()
            viewModel.handleEvent(FinancialManagementEvent.SaveOperation)
            assertEquals("My reviewed change", viewModel.state.value.operationDraft?.descriptionText)
            assertEquals(remote.version, viewModel.state.value.conflictingOperation?.version)
            assertIs<FinancialError.OperationConflict>(viewModel.state.value.operationError)

            viewModel.handleEvent(FinancialManagementEvent.RetryConflictingOperation)
            assertEquals(remote.version, viewModel.state.value.operationDraft?.original?.version)
            viewModel.handleEvent(FinancialManagementEvent.SaveOperation)
            runCurrent()
            val saved = gateway.observeFinancialSnapshot(org.orev.nahidka.feature.financial.dto.MonthlyQuery(YearMonth(2024, 5), "iso4217:USD")).first()
                .operations.single()
            assertEquals(2_000, saved.amount.units)
            assertEquals("My reviewed change", saved.description)
            assertNull(viewModel.state.value.operationDraft)
        }
    }

    private suspend fun TestScope.withFinance(
        seed: suspend (InMemoryFinancialGateway, org.orev.nahidka.feature.financial.dto.FinancialCategory, Instant) -> Unit = { _, _, _ -> },
        block: suspend (FinancialManagementViewModel, InMemoryFinancialGateway, org.orev.nahidka.feature.financial.dto.FinancialCategory) -> Unit,
    ) {
        val asset = AssetDefinition("iso4217:USD", "USD", 2)
        val config = FinancialSessionConfig("finance-ui-test", "finance-workspace", persistentListOf(asset), asset.id, "Europe/Kyiv")
        val gateway = InMemoryFinancialGateway(config, FinancialErrorReporter { }, FinancialCalendar())
        val categoryResult = gateway.createCategory(CreateFinancialCategory(CommandMeta("create-ui-category"), "ui-food", "Food"))
        val category = assertIs<MutationResult.Committed<org.orev.nahidka.feature.financial.dto.FinancialCategory>>(categoryResult).value
        val instant = Instant.parse("2024-05-12T10:30:00Z")
        seed(gateway, category, instant)
        var generated = 0
        val viewModelStore = ViewModelStore()
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val context = FinancialContext(gateway)
            val planningContext = FinancialPlanningContext(gateway)
            val finance = FinancialModule(
                FinancialService(gateway),
                context,
                FinancialPlanningService(gateway),
                planningContext,
                org.orev.nahidka.feature.financial.service.FinancialCategoryService(gateway),
                FinancialCategoryContext(gateway),
            )
            val model = FinancialManagementViewModel(
                finance,
                config,
                FinancialClock { instant },
                { "ui-${++generated}" },
                ExactMoneyFormatter(),
            )
            viewModelStore.put("financial-management-test", model)
            runCurrent()
            block(model, gateway, category)
        } finally {
            viewModelStore.clear()
            gateway.close()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
