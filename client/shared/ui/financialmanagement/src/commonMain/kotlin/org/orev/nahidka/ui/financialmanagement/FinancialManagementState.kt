package org.orev.nahidka.ui.financialmanagement

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialSnapshot

data class FinancialManagementState(
    val selectedMonth: YearMonth,
    val selectedAssetIdentifier: String,
    val snapshot: FinancialSnapshot? = null,
    val operations: PersistentList<FinancialOperation> = persistentListOf(),
    val selectedIdentifiers: PersistentSet<String> = persistentSetOf(),
    val operationDraft: FinancialOperationDraft? = null,
    val conflictingOperation: FinancialOperation? = null,
    val operationError: FinancialError? = null,
    val isSavingOperation: Boolean = false,
    val isCategoryEditorOpen: Boolean = false,
    val isPlanningEditorOpen: Boolean = false,
    val isSavingPlanning: Boolean = false,
    val operationSaveToken: Int = 0,
    val categorySaveToken: Int = 0,
    val planningSaveToken: Int = 0,
    val isLoading: Boolean = true,
    val feedback: String? = null,
)
