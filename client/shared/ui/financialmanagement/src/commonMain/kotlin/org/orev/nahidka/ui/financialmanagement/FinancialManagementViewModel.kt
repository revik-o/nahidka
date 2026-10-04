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
import org.orev.nahidka.core.common.nullablePatch
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
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.ui.common.state.StateHolder

class FinancialManagementViewModel @Inject constructor(
    private val financialModule: FinancialModule,
    private val financialSessionConfiguration: FinancialSessionConfig,
    private val applicationClock: ApplicationClock,
    private val identifierGenerator: IdentifierGenerator,
    private val exactMoneyFormatter: ExactMoneyFormatter,
) : StateHolder<FinancialManagementState, FinancialManagementEvent>() {
    private val _state = MutableStateFlow(FinancialManagementState(selectedMonth = currentMonth(), selectedAssetIdentifier = financialSessionConfiguration.defaultAssetIdentifier))
    override val state: StateFlow<FinancialManagementState> = _state.asStateFlow()
    private var snapshotJob: Job? = null

    init {
        observeMonth(_state.value.selectedMonth, _state.value.selectedAssetIdentifier)
        financialModule.financialGateway.subscribeOperations(OperationQuery())
            .onSnapshot { snapshot -> replaceOperations(snapshot.entities) }
            .onInsert { change -> updateOperationList(change.after) }
            .onUpdate { change -> updateOperationList(change.after) }
            .onDelete { change -> removeOperationFromList(change.before.identifier) }
            .onResync { snapshot -> replaceOperations(snapshot.entities) }
            .onError { failure -> setFeedback(failure.message ?: "Could not observe financial operations") }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: FinancialManagementEvent) {
        when (event) {
            FinancialManagementEvent.OpenAddOperation -> openAddOperation()
            is FinancialManagementEvent.EditOperation -> openEditOperation(event.identifier)
            is FinancialManagementEvent.UpdateOperationDraft -> _state.update { it.copy(operationDraft = event.draft, feedback = null) }
            FinancialManagementEvent.SaveOperation -> _state.value.operationDraft?.let(::saveOperation)
            FinancialManagementEvent.CloseOperationEditor -> _state.update { it.copy(operationDraft = null, conflictingOperation = null, operationError = null, feedback = null) }
            FinancialManagementEvent.ReloadConflictingOperation -> reloadConflictingOperation()
            FinancialManagementEvent.RetryConflictingOperation -> retryConflictingOperation()
            is FinancialManagementEvent.ToggleSelection -> _state.update {
                it.copy(selectedIdentifiers = if (event.identifier in it.selectedIdentifiers) it.selectedIdentifiers.removing(event.identifier) else it.selectedIdentifiers.adding(event.identifier))
            }
            is FinancialManagementEvent.RemoveOperation -> removeOperation(event.identifier)
            is FinancialManagementEvent.SelectMonth -> selectMonth(event.month)
            is FinancialManagementEvent.SelectAsset -> selectAsset(event.assetIdentifier)
            FinancialManagementEvent.OpenCategoryEditor -> _state.update { it.copy(isCategoryEditorOpen = true, feedback = null) }
            FinancialManagementEvent.CloseCategoryEditor -> _state.update { it.copy(isCategoryEditorOpen = false) }
            is FinancialManagementEvent.CreateCategory -> createCategory(event.name, event.iconName)
            is FinancialManagementEvent.RenameCategory -> renameCategory(event.identifier, event.expectedVersion, event.name)
            is FinancialManagementEvent.ArchiveCategory -> archiveCategory(event.identifier, event.expectedVersion)
            is FinancialManagementEvent.DeleteCategory -> deleteCategory(event.identifier, event.expectedVersion)
            FinancialManagementEvent.OpenPlanningEditor -> _state.update { it.copy(isPlanningEditorOpen = true, feedback = null) }
            FinancialManagementEvent.ClosePlanningEditor -> _state.update { it.copy(isPlanningEditorOpen = false) }
            is FinancialManagementEvent.SavePlanningTable -> savePlanningTable(event.table, event.expectedVersion)
            FinancialManagementEvent.ClearFeedback -> _state.update { it.copy(feedback = null) }
        }
    }

    val reportingTimeZone: TimeZone get() = TimeZone.of(financialSessionConfiguration.reportingTimeZone)

    fun newIdentifier(): String = identifierGenerator.next()

    val supportedAssets get() = financialSessionConfiguration.assets

    fun assetDefinition(identifier: String) = financialSessionConfiguration.assets.firstOrNull { it.identifier == identifier }

    fun amountInput(money: Money): String =
        assetDefinition(money.assetIdentifier)?.let { formatMoneyInput(money, it) } ?: money.units.toString()

    fun now() = applicationClock.now()

    fun formatMoney(money: Money): String {
        val moneyAsset = financialSessionConfiguration.assets.firstOrNull { it.identifier == money.assetIdentifier } ?: return "${money.units} ${money.assetIdentifier}"
        return exactMoneyFormatter.format(money, moneyAsset)
    }

    private fun currentMonth(): YearMonth {
        val localDate = applicationClock.now().toLocalDateTime(TimeZone.of(financialSessionConfiguration.reportingTimeZone)).date
        return YearMonth(localDate.year, localDate.month.ordinal + 1)
    }

    private fun selectMonth(month: YearMonth) {
        if (month == _state.value.selectedMonth) return
        _state.update { it.copy(selectedMonth = month, snapshot = null, selectedIdentifiers = persistentSetOf(), isLoading = true) }
        observeMonth(month, _state.value.selectedAssetIdentifier)
    }

    private fun selectAsset(assetIdentifier: String) {
        if (financialSessionConfiguration.assets.none { it.identifier == assetIdentifier } || assetIdentifier == _state.value.selectedAssetIdentifier) return
        _state.update { it.copy(selectedAssetIdentifier = assetIdentifier, snapshot = null, selectedIdentifiers = persistentSetOf(), isLoading = true) }
        observeMonth(_state.value.selectedMonth, assetIdentifier)
    }

    private fun observeMonth(month: YearMonth, assetIdentifier: String) {
        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            try {
                financialModule.financialGateway.observeFinancialSnapshot(MonthlyQuery(month, assetIdentifier)).collect { snapshot ->
                    _state.update { current ->
                        if (current.selectedMonth == month && current.selectedAssetIdentifier == assetIdentifier) current.copy(snapshot = snapshot, isLoading = false)
                        else current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { current ->
                    if (current.selectedMonth == month && current.selectedAssetIdentifier == assetIdentifier) {
                        current.copy(snapshot = null, isLoading = false, feedback = failure.message ?: "Could not load financial data")
                    } else current
                }
            }
        }
    }

    private fun replaceOperations(operations: List<FinancialOperation>) {
        _state.update { it.copy(operations = operations.sortedWith(compareByDescending<FinancialOperation> { operation -> operation.occurredAt }.thenBy { operation -> operation.identifier }).toPersistentList()) }
    }

    private fun updateOperationList(operation: FinancialOperation) {
        _state.update { current ->
            current.copy(
                operations = (current.operations.filterNot { it.identifier == operation.identifier } + operation)
                    .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.identifier })
                    .toPersistentList(),
            )
        }
    }

    private fun removeOperationFromList(identifier: String) {
        _state.update { current -> current.copy(operations = current.operations.filterNot { it.identifier == identifier }.toPersistentList()) }
    }

    private fun openAddOperation() {
        _state.update { current ->
            val firstCategory = current.snapshot?.categories?.firstOrNull { !it.archived }
            current.copy(
                operationDraft = FinancialOperationDraft(
                    original = null,
                    amountText = "",
                    assetIdentifier = financialSessionConfiguration.defaultAssetIdentifier,
                    kind = OperationKind.EXPENSE,
                    categoryIdentifier = firstCategory?.identifier,
                    paymentMethod = PaymentMethod.CARD,
                    occurredAtInstant = applicationClock.now(),
                    descriptionText = "",
                    refundOfOperationIdentifier = null,
                ),
                feedback = null,
                operationError = null,
            )
        }
    }

    private fun openEditOperation(identifier: String) {
        val operation = _state.value.operations.firstOrNull { it.identifier == identifier } ?: return
        val currentAssetDefinition = financialSessionConfiguration.assets.firstOrNull { it.identifier == operation.amount.assetIdentifier }
        if (currentAssetDefinition == null) {
            setFeedback("Asset ${operation.amount.assetIdentifier} is not supported in this session")
            return
        }
        _state.update {
            it.copy(
                operationDraft = FinancialOperationDraft(
                    original = operation,
                    amountText = formatMoneyInput(operation.amount, currentAssetDefinition),
                    assetIdentifier = operation.amount.assetIdentifier,
                    kind = operation.kind,
                    categoryIdentifier = operation.categoryIdentifier,
                    paymentMethod = operation.paymentMethod,
                    occurredAtInstant = operation.occurredAt,
                    descriptionText = operation.description.orEmpty(),
                    refundOfOperationIdentifier = operation.refundOfOperationIdentifier,
                ),
                conflictingOperation = null,
                operationError = null,
                feedback = null,
            )
        }
    }

    private fun reloadConflictingOperation() {
        val current = _state.value.conflictingOperation ?: return
        val currentAsset = financialSessionConfiguration.assets.firstOrNull { it.identifier == current.amount.assetIdentifier } ?: return
        _state.update {
            it.copy(
                operationDraft = FinancialOperationDraft(
                    original = current,
                    amountText = formatMoneyInput(current.amount, currentAsset),
                    assetIdentifier = current.amount.assetIdentifier,
                    kind = current.kind,
                    categoryIdentifier = current.categoryIdentifier,
                    paymentMethod = current.paymentMethod,
                    occurredAtInstant = current.occurredAt,
                    descriptionText = current.description.orEmpty(),
                    refundOfOperationIdentifier = current.refundOfOperationIdentifier,
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

    private fun removeOperation(identifier: String) {
        val operation = _state.value.operations.firstOrNull { it.identifier == identifier } ?: return
        viewModelScope.launch {
            when (val result = financialModule.financialGateway.removeOperation(
                RemoveFinancialOperation(CommandMeta(identifierGenerator.next()), operation.identifier, operation.version),
            )) {
                is MutationResult.Committed -> setFeedback("Operation removed")
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun saveOperation(draft: FinancialOperationDraft) {
        if (draft.original != null && draft.kind != draft.original.kind) {
            setFeedback("An existing operation's kind cannot change. Create a new operation instead.")
            return
        }
        val selectedAsset = financialSessionConfiguration.assets.firstOrNull { it.identifier == draft.assetIdentifier }
        if (selectedAsset == null) {
            setFeedback("Choose a supported asset")
            return
        }
        val amount = parseMoneyText(draft.amountText, selectedAsset)
        if (amount == null || amount.units <= 0) {
            setFeedback("Enter a positive amount with up to ${selectedAsset.fractionDigits} decimal places")
            return
        }
        val occurredAt = draft.occurredAtInstant
        val refundParent = if (draft.kind == OperationKind.REFUND) {
            _state.value.operations.firstOrNull { it.identifier == draft.refundOfOperationIdentifier && it.kind == OperationKind.EXPENSE && it.amount.assetIdentifier == selectedAsset.identifier }
                ?: run {
                    setFeedback("Select the expense that this refund belongs to")
                    return
                }
        } else null
        val categoryIdentifier = if (refundParent != null) refundParent.categoryIdentifier else draft.categoryIdentifier
        val cleanedDescription = draft.descriptionText.takeIf { it.isNotBlank() }
        if (_state.value.operationDraft == null) return
        _state.update { it.copy(isSavingOperation = true, feedback = null) }
        viewModelScope.launch {
            try {
                val result = if (draft.original == null) {
                    financialModule.financialGateway.addOperation(
                        AddFinancialOperation(
                            meta = CommandMeta(identifierGenerator.next()),
                            operation = NewFinancialOperation(
                                identifier = identifierGenerator.next(),
                                amount = amount,
                                kind = draft.kind,
                                categoryIdentifier = categoryIdentifier,
                                paymentMethod = draft.paymentMethod,
                                occurredAt = occurredAt,
                                description = cleanedDescription,
                                refundOfOperationIdentifier = refundParent?.identifier,
                            ),
                        ),
                    )
                } else {
                    financialModule.financialGateway.updateOperation(
                        UpdateFinancialOperation(
                            meta = CommandMeta(identifierGenerator.next()),
                            identifier = draft.original.identifier,
                            expectedVersion = draft.original.version,
                            patch = FinancialOperationPatch(
                                amount = amount.takeIf { it != draft.original.amount },
                                categoryIdentifier = nullablePatch(draft.original.categoryIdentifier, categoryIdentifier),
                                paymentMethod = draft.paymentMethod.takeIf { it != draft.original.paymentMethod },
                                occurredAt = occurredAt.takeIf { it != draft.original.occurredAt },
                                description = nullablePatch(draft.original.description, cleanedDescription),
                                refundOfOperationIdentifier = nullablePatch(draft.original.refundOfOperationIdentifier, refundParent?.identifier),
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
            when (val result = financialModule.financialGateway.createCategory(
                CreateFinancialCategory(CommandMeta(identifierGenerator.next()), identifierGenerator.next(), name, iconName),
            )) {
                is MutationResult.Committed -> _state.update {
                    it.copy(isCategoryEditorOpen = false, categorySaveToken = it.categorySaveToken + 1, feedback = "Category created")
                }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun renameCategory(identifier: String, expectedVersion: Long, name: String) {
        viewModelScope.launch {
            when (val result = financialModule.financialGateway.updateCategory(
                UpdateFinancialCategory(CommandMeta(identifierGenerator.next()), identifier, expectedVersion, name),
            )) {
                is MutationResult.Committed -> _state.update { it.copy(categorySaveToken = it.categorySaveToken + 1, feedback = "Category renamed") }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun archiveCategory(identifier: String, expectedVersion: Long) {
        viewModelScope.launch {
            when (val result = financialModule.financialGateway.archiveCategory(
                ArchiveFinancialCategory(CommandMeta(identifierGenerator.next()), identifier, expectedVersion),
            )) {
                is MutationResult.Committed -> _state.update { it.copy(categorySaveToken = it.categorySaveToken + 1, feedback = "Category archived") }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun deleteCategory(identifier: String, expectedVersion: Long) {
        viewModelScope.launch {
            when (val result = financialModule.financialGateway.deleteCategory(
                DeleteFinancialCategory(CommandMeta(identifierGenerator.next()), identifier, expectedVersion),
            )) {
                is MutationResult.Committed -> _state.update { it.copy(categorySaveToken = it.categorySaveToken + 1, feedback = "Category deleted") }
                is MutationResult.Rejected -> setFeedback(describe(result.error))
            }
        }
    }

    private fun savePlanningTable(table: org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput, expectedVersion: Long?) {
        _state.update { it.copy(isSavingPlanning = true, feedback = null) }
        viewModelScope.launch {
            when (val result = financialModule.financialGateway.savePlanningTable(
                SavePlanningTable(CommandMeta(identifierGenerator.next()), expectedVersion, table),
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
        is FinancialError.NotFound -> "${error.entity} ${error.identifier} no longer exists"
        is FinancialError.OperationConflict -> "This operation changed elsewhere. The current version is ${error.current.version}; your draft is still open."
        is FinancialError.PlanningConflict -> "This monthly plan changed elsewhere. Reload the current table before saving again."
        is FinancialError.CategoryConflict -> "This category changed elsewhere. The current version is ${error.current.version}."
        is FinancialError.CategoryInUse -> "This category is used by operations or a plan. Archive it instead."
        is FinancialError.CommandIdentifierReused -> "A different command already used this retry ID."
        FinancialError.SessionClosed -> "This financial session is closed."
        FinancialError.StorageUnavailable -> "Financial storage is unavailable."
        FinancialError.Forbidden -> "This financial action is not permitted."
    }
}
