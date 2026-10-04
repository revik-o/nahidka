package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.ui.financialmanagement.FinancialViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FinancialPlanningViewModelTest : FinancialViewModelTest() {

    private val financialPlanningViewModel = financialManagementTestGraph.financialPlanningViewModel

    @Test
    fun planningRowCreationCreatesMonthlyTable() = runTest {
        createPlanningRow(plannedAmountText = "300", category = createCategory("Food"))

        val planningContent = financialPlanningViewModel.planning.awaitContent { content -> content.rows.size == 1 }

        assertEquals("Food", planningContent.rows.single().categoryName)
        assertEquals("300.00 USD", planningContent.rows.single().formattedAmount)
        assertEquals(
            FinancialPlanningSummary(
                formattedAvailableAmount = "0.00 USD",
                formattedRemainingAmount = "0.00 USD",
                formattedSavableAmount = "-300.00 USD",
            ),
            planningContent.summary,
        )
    }

    @Test
    fun planningRowDeletionKeepsEmptyTable() = runTest {
        createPlanningRow(plannedAmountText = "300", category = createCategory("Food"))
        val createdRow = financialPlanningViewModel.planning.awaitContent { content -> content.rows.size == 1 }.rows.single()

        financialPlanningViewModel.openPlanningRowDeletion(createdRow)
        financialPlanningViewModel.planningRowDeletion.submit()

        financialPlanningViewModel.planning.awaitContent { content -> content.rows.isEmpty() && content.planningTable != null }
    }

    @Test
    fun nextMonthSelectionObservesNextMonth() = runTest {
        financialPlanningViewModel.selectNextMonth()

        assertEquals(YearMonth(2026, 11), financialPlanningViewModel.selectedMonth.value)
        financialPlanningViewModel.planning.awaitContent { content -> content.month == YearMonth(2026, 11) }
    }

    @Test
    fun editingPlanningRowUpdatesAmountAndSummary() = runTest {
        createPlanningRow(plannedAmountText = "300", category = createCategory("Food"))
        val createdRow = financialPlanningViewModel.planning.awaitContent { content -> content.rows.size == 1 }.rows.single()
        financialPlanningViewModel.openPlanningRowEditing(createdRow)
        financialPlanningViewModel.planningRowEditor.edit { draft -> draft.copy(plannedAmountText = "125.50") }
        financialPlanningViewModel.planningRowEditor.submit()

        val updatedContent = financialPlanningViewModel.planning.awaitContent { content ->
            content.rows.singleOrNull()?.formattedAmount == "125.50 USD"
        }
        assertEquals("-125.50 USD", updatedContent.summary.formattedSavableAmount)
        assertEquals(createdRow.planningRow.identifier, updatedContent.rows.single().planningRow.identifier)
    }

    @Test
    fun plannedCategoriesAreExcludedWhileEditingKeepsCurrentCategory() = runTest {
        val foodCategory = createCategory("Food")
        val homeCategory = createCategory("Home")
        createPlanningRow(plannedAmountText = "300", category = foodCategory)
        val content = financialPlanningViewModel.planning.awaitContent { planning -> planning.rows.size == 1 }
        financialPlanningViewModel.openPlanningRowCreation()
        val creationDraft = checkNotNull(financialPlanningViewModel.planningRowEditor.dialogState.value).draft
        assertEquals(listOf(homeCategory), creationDraft.selectableCategories(content.categories))
        financialPlanningViewModel.openPlanningRowEditing(content.rows.single())
        val editingDraft = checkNotNull(financialPlanningViewModel.planningRowEditor.dialogState.value).draft
        assertEquals(listOf(foodCategory, homeCategory), editingDraft.selectableCategories(content.categories))
    }

    private suspend fun createPlanningRow(plannedAmountText: String, category: FinancialCategory) {
        financialPlanningViewModel.planning.awaitContent { true }
        financialPlanningViewModel.openPlanningRowCreation()
        financialPlanningViewModel.planningRowEditor.edit { planningRowDraft ->
            planningRowDraft.copy(plannedAmountText = plannedAmountText, category = category)
        }
        financialPlanningViewModel.planningRowEditor.submit()
    }
}
