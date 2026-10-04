package org.orev.nahidka.ui.financialmanagement

import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.SavePlanningTable

sealed interface FinancialManagementEvent {
    data object OpenAddOperation : FinancialManagementEvent
    data class EditOperation(val identifier: String) : FinancialManagementEvent
    data class UpdateOperationDraft(val draft: FinancialOperationDraft) : FinancialManagementEvent
    data object SaveOperation : FinancialManagementEvent
    data object CloseOperationEditor : FinancialManagementEvent
    data object ReloadConflictingOperation : FinancialManagementEvent
    data object RetryConflictingOperation : FinancialManagementEvent
    data class ToggleSelection(val identifier: String) : FinancialManagementEvent
    data class RemoveOperation(val identifier: String) : FinancialManagementEvent
    data class SelectMonth(val month: YearMonth) : FinancialManagementEvent
    data class SelectAsset(val assetIdentifier: String) : FinancialManagementEvent
    data object OpenCategoryEditor : FinancialManagementEvent
    data object CloseCategoryEditor : FinancialManagementEvent
    data class CreateCategory(val name: String, val iconName: String?) : FinancialManagementEvent
    data class RenameCategory(val identifier: String, val expectedVersion: Long, val name: String) : FinancialManagementEvent
    data class ArchiveCategory(val identifier: String, val expectedVersion: Long) : FinancialManagementEvent
    data class DeleteCategory(val identifier: String, val expectedVersion: Long) : FinancialManagementEvent
    data object OpenPlanningEditor : FinancialManagementEvent
    data object ClosePlanningEditor : FinancialManagementEvent
    data class SavePlanningTable(val table: FinancialPlanningTableInput, val expectedVersion: Long?) : FinancialManagementEvent
    data object ClearFeedback : FinancialManagementEvent
}
