package org.orev.nahidka.ui.financialmanagement.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.financial.calculation.FinancialCategoryNameComparator
import org.orev.nahidka.feature.financial.calculation.FinancialOperationRecencyComparator
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.ui.financialmanagement.FinancialAssetSelection
import org.orev.nahidka.ui.financialmanagement.common.*
import kotlin.time.Instant

private const val FINANCIAL_HISTORY_PAGE_SIZE = 50

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class FinancialHistoryViewModel(
    financialSessionConfig: FinancialSessionConfig,
    private val financialGateway: FinancialGateway,
    private val financialAssetSelection: FinancialAssetSelection,
    private val identifierGenerator: IdentifierGenerator,
    private val applicationClock: ApplicationClock,
    private val exactMoneyFormatter: ExactMoneyFormatter,
) : ViewModel() {

    private val reportingTimeZone = TimeZone.of(financialSessionConfig.reportingTimeZone)

    private val displayedRowLimit = MutableStateFlow(FINANCIAL_HISTORY_PAGE_SIZE)

    val availableAssets: PersistentList<AssetDefinition> = financialAssetSelection.availableAssets

    val selectedAsset: StateFlow<AssetDefinition> = financialAssetSelection.selectedAsset

    val history: StateFlow<FinancialContentState<FinancialHistoryContent>> = financialAssetSelection.selectedAsset
        .flatMapLatest { selectedAsset -> observeHistory(selectedAsset) }
        .stateInFinancialContent(viewModelScope)

    val operationEditor = FinancialDialogController(viewModelScope, FinancialOperationDraft::submittable) { operationDraft ->
        val editedOperation = operationDraft.editedOperation

        if (editedOperation == null) {
            addOperation(operationDraft)
        } else {
            updateOperation(editedOperation, operationDraft)
        }
    }

    val operationDeletion = FinancialDialogController<FinancialOperationRow>(viewModelScope) { operationRow ->
        financialGateway.removeOperation(
            RemoveFinancialOperation(
                meta = identifierGenerator.nextCommandMeta(),
                identifier = operationRow.operation.identifier,
                expectedVersion = operationRow.operation.version,
            ),
        )
    }

    fun selectAsset(asset: AssetDefinition) {
        displayedRowLimit.value = FINANCIAL_HISTORY_PAGE_SIZE
        financialAssetSelection.select(asset)
    }

    fun loadNextPage() {
        displayedRowLimit.update { rowLimit -> rowLimit + FINANCIAL_HISTORY_PAGE_SIZE }
    }

    fun openOperationCreation() {
        operationEditor.open(
            FinancialOperationDraft(
                editedOperation = null,
                asset = selectedAsset.value,
                kind = OperationKind.EXPENSE,
                amountText = "",
                category = null,
                paymentMethod = PaymentMethod.CARD,
                occurredOn = applicationClock.now().toLocalDateTime(reportingTimeZone).date,
            ),
        )
    }

    fun openOperationEditing(operationRow: FinancialOperationRow) {
        val operationAsset = availableAssets.first { asset ->
            asset.identifier == operationRow.operation.amount.assetIdentifier
        }
        operationEditor.open(
            FinancialOperationDraft(
                editedOperation = operationRow.operation,
                asset = operationAsset,
                kind = operationRow.operation.kind,
                amountText = formatMoneyInput(operationRow.operation.amount, operationAsset),
                category = operationRow.category,
                paymentMethod = operationRow.operation.paymentMethod,
                occurredOn = operationRow.occurredOn,
            ),
        )
    }

    fun openOperationDeletion(operationRow: FinancialOperationRow) {
        operationDeletion.open(operationRow)
    }

    private fun observeHistory(selectedAsset: AssetDefinition): Flow<FinancialHistoryContent> = combine(
        financialGateway
            .subscribeOperations(OperationQuery(assetIdentifier = selectedAsset.identifier))
            .observeEntities(FinancialOperation::identifier),
        financialGateway
            .subscribeCategories(CategoryQuery())
            .observeEntities(FinancialCategory::identifier),
        displayedRowLimit,
    ) { operationsByIdentifier, categoriesByIdentifier, rowLimit ->
        toHistoryContent(selectedAsset, operationsByIdentifier, categoriesByIdentifier, rowLimit)
    }

    private fun toHistoryContent(
        selectedAsset: AssetDefinition,
        operationsByIdentifier: PersistentMap<String, FinancialOperation>,
        categoriesByIdentifier: PersistentMap<String, FinancialCategory>,
        rowLimit: Int,
    ): FinancialHistoryContent {
        val sortedOperations = operationsByIdentifier.values.sortedWith(FinancialOperationRecencyComparator)

        return FinancialHistoryContent(
            rows = sortedOperations
                .take(rowLimit)
                .map { operation ->
                    FinancialOperationRow(
                        operation = operation,
                        category = categoriesByIdentifier[operation.categoryIdentifier],
                        formattedAmount = exactMoneyFormatter.format(operation.amount, selectedAsset),
                        occurredOn = operation.occurredAt.toLocalDateTime(reportingTimeZone).date,
                    )
                }
                .toPersistentList(),
            hasMoreRows = sortedOperations.size > rowLimit,
            categories = categoriesByIdentifier.values
                .sortedWith(FinancialCategoryNameComparator)
                .toPersistentList(),
        )
    }

    private suspend fun addOperation(operationDraft: FinancialOperationDraft): MutationResult<FinancialOperation> =
        financialGateway.addOperation(
            AddFinancialOperation(
                meta = identifierGenerator.nextCommandMeta(),
                operation = NewFinancialOperation(
                    identifier = identifierGenerator.next(),
                    amount = checkNotNull(operationDraft.amount),
                    kind = operationDraft.kind,
                    categoryIdentifier = checkNotNull(operationDraft.category).identifier,
                    paymentMethod = operationDraft.paymentMethod,
                    occurredAt = occurredAt(operationDraft),
                ),
            ),
        )

    private suspend fun updateOperation(
        editedOperation: FinancialOperation,
        operationDraft: FinancialOperationDraft,
    ): MutationResult<FinancialOperation> = financialGateway.updateOperation(
        UpdateFinancialOperation(
            meta = identifierGenerator.nextCommandMeta(),
            identifier = editedOperation.identifier,
            expectedVersion = editedOperation.version,
            patch = FinancialOperationPatch(
                amount = operationDraft.amount,
                categoryIdentifier = NullablePatch.Set(checkNotNull(operationDraft.category).identifier),
                paymentMethod = operationDraft.paymentMethod,
                occurredAt = occurredAt(operationDraft),
            ),
        ),
    )

    private fun occurredAt(operationDraft: FinancialOperationDraft): Instant {
        val referenceInstant = operationDraft.editedOperation?.occurredAt ?: applicationClock.now()
        val referenceDateTime = referenceInstant.toLocalDateTime(reportingTimeZone)

        return if (referenceDateTime.date == operationDraft.occurredOn) {
            referenceInstant
        } else {
            operationDraft.occurredOn
                .atTime(referenceDateTime.time)
                .toInstant(reportingTimeZone)
        }
    }
}
