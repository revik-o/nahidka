package org.orev.nahidka.ui.financialmanagement

import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.calculation.parseMoneyText
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.command.NullablePatch
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.feature.financial.support.FinancialClock
import org.orev.nahidka.feature.financial.support.FinancialIdGenerator
import org.orev.nahidka.ui.common.state.StateHolder

class FinancialManagementViewModel @Inject constructor(
    private val finance: FinancialModule,
    private val config: FinancialSessionConfig,
    private val clock: FinancialClock,
    private val ids: FinancialIdGenerator,
    private val moneyFormatter: ExactMoneyFormatter,
) : StateHolder<FinancialManagementState, FinancialManagementEvent>() {
    private val _state = MutableStateFlow(FinancialManagementState(selectedMonth = currentMonth(), selectedAssetId = config.defaultAssetId))
    override val state: StateFlow<FinancialManagementState> = _state.asStateFlow()
    private var snapshotJob: Job? = null

    init {
        observeMonth(_state.value.selectedMonth, _state.value.selectedAssetId)
        finance.financialContext.subscribe()
            .onSnapshot { snapshot -> replaceOperations(snapshot.entities) }
            .onInsert { change -> updateOperationList(change.after) }
            .onUpdate { change -> updateOperationList(change.after) }
            .onDelete { change -> removeOperationFromList(change.before.id) }
            .onResync { snapshot -> replaceOperations(snapshot.entities) }
            .onError { failure -> setFeedback(failure.message ?: "Could not observe financial operations") }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: FinancialManagementEvent) {
        when (event) {
            FinancialManagementEvent.OpenAddOperation -> openAddOperation()
            is FinancialManagementEvent.EditOperation -> openEditOperation(event.id)
            is FinancialManagementEvent.UpdateOperationDraft -> _state.update { it.copy(operationDraft = event.draft, feedback = null) }
            FinancialManagementEvent.SaveOperation -> _state.value.operationDraft?.let(::saveOperation)
            FinancialManagementEvent.CloseOperationEditor -> _state.update { it.copy(operationDraft = null, conflictingOperation = null, operationError = null, feedback = null) }
            FinancialManagementEvent.ReloadConflictingOperation -> reloadConflictingOperation()
            FinancialManagementEvent.RetryConflictingOperation -> retryConflictingOperation()
            is FinancialManagementEvent.ToggleSelection -> _state.update {
                it.copy(selectedIds = if (event.id in it.selectedIds) it.selectedIds.removing(event.id) else it.selectedIds.adding(event.id))
            }
            is FinancialManagementEvent.RemoveOperation -> removeOperation(event.id)
            is FinancialManagementEvent.SelectMonth -> selectMonth(event.month)
            is FinancialManagementEvent.SelectAsset -> selectAsset(event.assetId)
            FinancialManagementEvent.OpenCategoryEditor -> _state.update { it.copy(isCategoryEditorOpen = true, feedback = null) }
            FinancialManagementEvent.CloseCategoryEditor -> _state.update { it.copy(isCategoryEditorOpen = false) }
            is FinancialManagementEvent.CreateCategory -> createCategory(event.name, event.iconName)
            is FinancialManagementEvent.RenameCategory -> renameCategory(event.id, event.expectedVersion, event.name)
            is FinancialManagementEvent.ArchiveCategory -> archiveCategory(event.id, event.expectedVersion)
            is FinancialManagementEvent.DeleteCategory -> deleteCategory(event.id, event.expectedVersion)
            FinancialManagementEvent.OpenPlanningEditor -> _state.update { it.copy(isPlanningEditorOpen = true, feedback = null) }
            FinancialManagementEvent.ClosePlanningEditor -> _state.update { it.copy(isPlanningEditorOpen = false) }
            is FinancialManagementEvent.SavePlanningTable -> savePlanningTable(event.table, event.expectedVersion)
            FinancialManagementEvent.ClearFeedback -> _state.update { it.copy(feedback = null) }
        }
    }

    fun newId(): String = ids.next()

    val supportedAssets get() = config.assets

    fun assetDefinition(id: String) = config.assets.firstOrNull { it.id == id }

    fun amountInput(money: Money): String =
        assetDefinition(money.assetId)?.let { formatMoneyInput(money, it) } ?: money.units.toString()

    fun now() = clock.now()

    fun formatMoney(money: Money): String {
        val moneyAsset = config.assets.firstOrNull { it.id == money.assetId } ?: return "${money.units} ${money.assetId}"
        return moneyFormatter.format(money, moneyAsset)
    }

    private fun currentMonth(): YearMonth {
        val localDate = clock.now().toLocalDateTime(TimeZone.of(config.reportingTimeZone)).date
        return YearMonth(localDate.year, localDate.month.ordinal + 1)
    }

    private fun selectMonth(month: YearMonth) {
        if (month == _state.value.selectedMonth) return
        _state.update { it.copy(selectedMonth = month, snapshot = null, selectedIds = persistentSetOf(), isLoading = true) }
        observeMonth(month, _state.value.selectedAssetId)
    }

    private fun selectAsset(assetId: String) {
        if (config.assets.none { it.id == assetId } || assetId == _state.value.selectedAssetId) return
        _state.update { it.copy(selectedAssetId = assetId, snapshot = null, selectedIds = persistentSetOf(), isLoading = true) }
        observeMonth(_state.value.selectedMonth, assetId)
    }

    private fun observeMonth(month: YearMonth, assetId: String) {
        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            try {
                finance.financialContext.observeFinancialSnapshot(MonthlyQuery(month, assetId)).collect { snapshot ->
                    _state.update { current ->
                        if (current.selectedMonth == month && current.selectedAssetId == assetId) current.copy(snapshot = snapshot, isLoading = false)
                        else current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { current ->
                    if (current.selectedMonth == month && current.selectedAssetId == assetId) {
                        current.copy(snapshot = null, isLoading = false, feedback = failure.message ?: "Could not load financial data")
                    } else current
                }
            }
        }
    }

    private fun replaceOperations(operations: List<FinancialOperation>) {
        _state.update { it.copy(operations = operations.sortedWith(compareByDescending<FinancialOperation> { operation -> operation.occurredAt }.thenBy { operation -> operation.id }).toPersistentList()) }
    }

    private fun updateOperationList(operation: FinancialOperation) {
        _state.update { current ->
            current.copy(
                operations = (current.operations.filterNot { it.id == operation.id } + operation)
                    .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.id })
                    .toPersistentList(),
            )
        }
    }

    private fun removeOperationFromList(id: String) {
        _state.update { current -> current.copy(operations = current.operations.filterNot { it.id == id }.toPersistentList()) }
    }

    private fun openAddOperation() {
        _state.update { current ->
            val firstCategory = current.snapshot?.categories?.firstOrNull { !it.archived }
            current.copy(
                operationDraft = FinancialOperationDraft(
                    original = null,
                    amountText = "",
                    assetId = config.defaultAssetId,
                    kind = OperationKind.EXPENSE,
                    categoryId = firstCategory?.id,
                    paymentMethod = PaymentMethod.CARD,
                    occurredAtText = clock.now().toString(),
                    descriptionText = "",
                    refundOfOperationId = null,
                ),
                feedback = null,
                operationError = null,
            )
        }
    }

    private fun openEditOperation(id: String) {
        val operation = _state.value.operations.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                operationDraft = FinancialOperationDraft(
                    original = operation,
                    amountText = formatMoneyInput(operation.amount, config.assets.first { it.id == operation.amount.assetId }),
                    assetId = operation.amount.assetId,
                    kind = operation.kind,
                    categoryId = operation.categoryId,
                    paymentMethod = operation.paymentMethod,
                    occurredAtText = operation.occurredAt.toString(),
                    descriptionText = operation.description.orEmpty(),
                    refundOfOperationId = operation.refundOfOperationId,
                ),
                conflictingOperation = null,
                operationError = null,
                feedback = null,
            )
        }
    }

    private fun reloadConflictingOperation() {
        val current = _state.value.conflictingOperation ?: return
        val currentAsset = config.assets.firstOrNull { it.id == current.amount.assetId } ?: return
        _state.update {
            it.copy(
                operationDraft = FinancialOperationDraft(
                    original = current,
                    amountText = formatMoneyInput(current.amount, currentAsset),
                    assetId = current.amount.assetId,
                    kind = current.kind,
                    categoryId = current.categoryId,
                    paymentMethod = current.paymentMethod,
                    occurredAtText = current.occurredAt.toString(),
                    descriptionText = current.description.orEmpty(),
                    refundOfOperationId = current.refundOfOperationId,
                ),
                conflictingOperation = null,
                feedback = null,
            )
        }
    }

    private fun retryConflictingOperation() {
        val current = _state.value.conflictingOperation ?: return
        _state.update { state ->
            val draft = state.operationDraft ?: return@update state
            state.copy(operationDraft = draft.copy(original = current), conflictingOperation = null, operationError = null, feedback = "Your reviewed edits will be saved against version ${current.version}.")
        }
    }

    private fun removeOperation(id: String) {
        val operation = _state.value.operations.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            when (val result = finance.financialManager.removeFinancialManipulation(
                RemoveFinancialOperation(CommandMeta(ids.next()), operation.id, operation.version),
            )) {
                is MutationResult.Committed -> setFeedback("Operation removed")
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun saveOperation(draft: FinancialOperationDraft) {
        val selectedAsset = config.assets.firstOrNull { it.id == draft.assetId }
        if (selectedAsset == null) {
            setFeedback("Choose a supported asset")
            return
        }
        val amount = parseMoneyText(draft.amountText, selectedAsset)
        if (amount == null || amount.units <= 0) {
            setFeedback("Enter a positive amount with up to ${selectedAsset.fractionDigits} decimal places")
            return
        }
        val occurredAt = try {
            Instant.parse(draft.occurredAtText.trim())
        } catch (_: IllegalArgumentException) {
            setFeedback("Enter the occurrence time as an ISO-8601 timestamp with an offset")
            return
        }
        val refundParent = if (draft.kind == OperationKind.REFUND) {
            _state.value.operations.firstOrNull { it.id == draft.refundOfOperationId && it.kind == OperationKind.EXPENSE && it.amount.assetId == selectedAsset.id }
                ?: run {
                    setFeedback("Select the expense that this refund belongs to")
                    return
                }
        } else null
        val categoryId = if (refundParent != null) refundParent.categoryId else draft.categoryId
        val cleanedDescription = draft.descriptionText.takeIf { it.isNotBlank() }
        if (_state.value.operationDraft == null) return
        _state.update { it.copy(isSavingOperation = true, feedback = null) }
        viewModelScope.launch {
            try {
                val result = if (draft.original == null) {
                    finance.financialManager.addNewFinancialManipulation(
                        AddFinancialOperation(
                            meta = CommandMeta(ids.next()),
                            operation = NewFinancialOperation(
                                id = ids.next(),
                                amount = amount,
                                kind = draft.kind,
                                categoryId = categoryId,
                                paymentMethod = draft.paymentMethod,
                                occurredAt = occurredAt,
                                description = cleanedDescription,
                                refundOfOperationId = refundParent?.id,
                            ),
                        ),
                    )
                } else {
                    finance.financialManager.updateFinancialManipulation(
                        UpdateFinancialOperation(
                            meta = CommandMeta(ids.next()),
                            id = draft.original.id,
                            expectedVersion = draft.original.version,
                            patch = FinancialOperationPatch(
                                amount = amount,
                                kind = draft.kind,
                                categoryId = categoryId?.let { NullablePatch.Set(it) } ?: NullablePatch.Clear,
                                paymentMethod = draft.paymentMethod,
                                occurredAt = occurredAt,
                                description = cleanedDescription?.let { NullablePatch.Set(it) } ?: NullablePatch.Clear,
                                refundOfOperationId = refundParent?.id?.let { NullablePatch.Set(it) } ?: NullablePatch.Clear,
                            ),
                        ),
                    )
                }
                when (result) {
                    is MutationResult.Committed -> _state.update {
                        it.copy(operationDraft = null, conflictingOperation = null, operationError = null, isSavingOperation = false, operationSaveToken = it.operationSaveToken + 1, feedback = "Operation saved")
                    }
                    is MutationResult.Rejected -> _state.update { state ->
                        state.copy(
                            isSavingOperation = false,
                            conflictingOperation = (result.error as? FinancialError.OperationConflict)?.current,
                            operationError = result.error,
                            feedback = describe(result.error),
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(isSavingOperation = false, feedback = failure.message ?: "Could not save operation") }
            }
        }
    }

    private fun createCategory(name: String, iconName: String?) {
        viewModelScope.launch {
            when (val result = finance.financialCategoryManager.createCategory(
                CreateFinancialCategory(CommandMeta(ids.next()), ids.next(), name, iconName),
            )) {
                is MutationResult.Committed -> _state.update {
                    it.copy(isCategoryEditorOpen = false, categorySaveToken = it.categorySaveToken + 1, feedback = "Category created")
                }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun renameCategory(id: String, expectedVersion: Long, name: String) {
        viewModelScope.launch {
            when (val result = finance.financialCategoryManager.updateCategory(
                UpdateFinancialCategory(CommandMeta(ids.next()), id, expectedVersion, name),
            )) {
                is MutationResult.Committed -> _state.update { it.copy(categorySaveToken = it.categorySaveToken + 1, feedback = "Category renamed") }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun archiveCategory(id: String, expectedVersion: Long) {
        viewModelScope.launch {
            when (val result = finance.financialCategoryManager.archiveCategory(
                ArchiveFinancialCategory(CommandMeta(ids.next()), id, expectedVersion),
            )) {
                is MutationResult.Committed -> _state.update { it.copy(categorySaveToken = it.categorySaveToken + 1, feedback = "Category archived") }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun deleteCategory(id: String, expectedVersion: Long) {
        viewModelScope.launch {
            when (val result = finance.financialCategoryManager.deleteCategory(
                DeleteFinancialCategory(CommandMeta(ids.next()), id, expectedVersion),
            )) {
                is MutationResult.Committed -> _state.update { it.copy(categorySaveToken = it.categorySaveToken + 1, feedback = "Category deleted") }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun savePlanningTable(table: org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput, expectedVersion: Long?) {
        _state.update { it.copy(isSavingPlanning = true, feedback = null) }
        viewModelScope.launch {
            when (val result = finance.financialPlanningManager.saveTable(
                SavePlanningTable(CommandMeta(ids.next()), expectedVersion, table),
            )) {
                is MutationResult.Committed -> _state.update {
                    it.copy(isSavingPlanning = false, isPlanningEditorOpen = false, planningSaveToken = it.planningSaveToken + 1, feedback = "Monthly plan saved")
                }
                is MutationResult.Rejected -> _state.update { it.copy(isSavingPlanning = false, feedback = describe(result.error)) }
            }
        }
    }

    private fun setFeedback(message: String) {
        _state.update { it.copy(feedback = message) }
    }

    private fun describe(error: FinancialError): String = when (error) {
        is FinancialError.Validation -> "${error.field}: ${error.message}"
        is FinancialError.NotFound -> "${error.entity} ${error.id} no longer exists"
        is FinancialError.OperationConflict -> "This operation changed elsewhere. The current version is ${error.current.version}; your draft is still open."
        is FinancialError.PlanningConflict -> "This monthly plan changed elsewhere. Reload the current table before saving again."
        is FinancialError.CategoryConflict -> "This category changed elsewhere. The current version is ${error.current.version}."
        is FinancialError.CategoryInUse -> "This category is used by operations or a plan. Archive it instead."
        is FinancialError.CommandIdReused -> "A different command already used this retry ID."
        FinancialError.SessionClosed -> "This financial session is closed."
        FinancialError.StorageUnavailable -> "Financial storage is unavailable."
        FinancialError.Forbidden -> "This financial action is not permitted."
    }
}
