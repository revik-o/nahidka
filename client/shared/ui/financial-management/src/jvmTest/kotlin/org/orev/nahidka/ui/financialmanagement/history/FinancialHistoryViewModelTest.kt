package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.coroutines.test.runTest
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.ui.financialmanagement.FINANCIAL_TEST_ASSET
import org.orev.nahidka.ui.financialmanagement.FinancialViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

internal class FinancialHistoryViewModelTest : FinancialViewModelTest() {

    private val financialHistoryViewModel = financialManagementTestGraph.financialHistoryViewModel

    @Test
    fun operationCreationAddsRow() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))

        val historyContent = financialHistoryViewModel.history.awaitContent { content ->
            content.rows.singleOrNull()?.categoryName == "Food"
        }

        assertEquals("12.50 USD", historyContent.rows.single().formattedAmount)
        assertNull(financialHistoryViewModel.operationEditor.dialogState.value)
    }

    @Test
    fun operationDraftWithoutCategoryIsNotSubmittable() = runTest {
        financialHistoryViewModel.openOperationCreation()
        financialHistoryViewModel.operationEditor.edit { operationDraft -> operationDraft.copy(amountText = "12.50") }

        assertFalse(checkNotNull(financialHistoryViewModel.operationEditor.dialogState.value).submittable)
    }

    @Test
    fun operationEditingUpdatesAmount() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))
        val createdRow = awaitCreatedRow()

        financialHistoryViewModel.openOperationEditing(createdRow)
        financialHistoryViewModel.operationEditor.edit { operationDraft -> operationDraft.copy(amountText = "20") }
        financialHistoryViewModel.operationEditor.submit()

        financialHistoryViewModel.history.awaitContent { content ->
            content.rows.singleOrNull()?.formattedAmount == "20.00 USD"
        }
    }

    @Test
    fun operationDeletionRemovesRow() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))
        val createdRow = awaitCreatedRow()

        financialHistoryViewModel.openOperationDeletion(createdRow)
        financialHistoryViewModel.operationDeletion.submit()

        financialHistoryViewModel.history.awaitContent { content -> content.rows.isEmpty() && !content.hasMoreRows }
    }

    @Test
    fun nextPageDisplaysRemainingRows() = runTest {
        val category = createCategory("Food")
        repeat(51) { operationIndex ->
            financialManagementTestGraph.financialGateway.addOperation(
                AddFinancialOperation(
                    meta = CommandMeta(identifierGenerator.next()),
                    operation = NewFinancialOperation(
                        identifier = identifierGenerator.next(),
                        amount = Money(FINANCIAL_TEST_ASSET.identifier, operationIndex + 1L),
                        kind = OperationKind.EXPENSE,
                        categoryIdentifier = category.identifier,
                        paymentMethod = PaymentMethod.CASH,
                        occurredAt = Instant.parse("2026-10-01T12:00:00Z"),
                    ),
                ),
            )
        }

        financialHistoryViewModel.history.awaitContent { content -> content.rows.size == 50 && content.hasMoreRows }
        financialHistoryViewModel.loadNextPage()

        financialHistoryViewModel.history.awaitContent { content -> content.rows.size == 51 && !content.hasMoreRows }
    }

    @Test
    fun assetSelectionIsSharedWithPlanningAndFiltersHistory() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))
        val originalRow = awaitCreatedRow()
        val planningViewModel = financialManagementTestGraph.financialPlanningViewModel
        val secondAsset = financialHistoryViewModel.availableAssets.last()

        financialHistoryViewModel.selectAsset(secondAsset)

        planningViewModel.planning.awaitContent { content -> content.asset == secondAsset }
        financialHistoryViewModel.history.awaitContent { content -> content.rows.isEmpty() }
        assertEquals(secondAsset, financialHistoryViewModel.selectedAsset.value)
        financialHistoryViewModel.openOperationCreation()
        assertEquals(secondAsset, financialHistoryViewModel.operationEditor.dialogState.value?.draft?.asset)
        financialHistoryViewModel.openOperationEditing(originalRow)
        val editingDraft = checkNotNull(financialHistoryViewModel.operationEditor.dialogState.value).draft
        assertEquals(FINANCIAL_TEST_ASSET, editingDraft.asset)
        assertEquals("12.50", editingDraft.amountText)

        financialHistoryViewModel.selectAsset(FINANCIAL_TEST_ASSET)
        awaitCreatedRow()
        planningViewModel.planning.awaitContent { content -> content.asset == FINANCIAL_TEST_ASSET }
    }

    private suspend fun awaitCreatedRow(): FinancialOperationRow = financialHistoryViewModel.history
        .awaitContent { content -> content.rows.singleOrNull()?.categoryName == "Food" }
        .rows
        .single()

    private fun createOperation(amountText: String, category: FinancialCategory) {
        financialHistoryViewModel.openOperationCreation()
        financialHistoryViewModel.operationEditor.edit { operationDraft ->
            operationDraft.copy(amountText = amountText, category = category)
        }
        financialHistoryViewModel.operationEditor.submit()
    }
}
