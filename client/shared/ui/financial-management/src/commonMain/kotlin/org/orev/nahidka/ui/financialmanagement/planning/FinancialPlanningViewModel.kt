package org.orev.nahidka.ui.financialmanagement.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.toInput
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.ui.financialmanagement.FinancialAssetSelection
import org.orev.nahidka.ui.financialmanagement.common.*

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class FinancialPlanningViewModel(
    financialSessionConfig: FinancialSessionConfig,
    applicationClock: ApplicationClock,
    financialAssetSelection: FinancialAssetSelection,
    private val financialGateway: FinancialGateway,
    private val identifierGenerator: IdentifierGenerator,
    private val exactMoneyFormatter: ExactMoneyFormatter,
) : ViewModel() {

    private val mutableSelectedMonth = MutableStateFlow(
        financialMonthFor(applicationClock.now(), financialSessionConfig.reportingTimeZone),
    )

    val selectedMonth: StateFlow<YearMonth> = mutableSelectedMonth.asStateFlow()

    val planning: StateFlow<FinancialContentState<FinancialPlanningContent>> =
        combine(mutableSelectedMonth, financialAssetSelection.selectedAsset) { month, asset ->
            MonthlyQuery(month, asset.identifier)
        }
            .flatMapLatest(financialGateway::observeFinancialSnapshot)
            .map(::toPlanningContent)
            .stateInFinancialContent(viewModelScope)

    val planningRowEditor = FinancialDialogController(viewModelScope, FinancialPlanningRowDraft::submittable) { planningRowDraft ->
        savePlanningRows(planningRowDraft) { planningRows ->
            planningRows.withPlanningRow(toPlanningRow(planningRowDraft))
        }
    }

    val planningRowDeletion = FinancialDialogController<FinancialPlanningRowDraft>(viewModelScope) { planningRowDraft ->
        savePlanningRows(planningRowDraft) { planningRows ->
            planningRows.removingAll { planningRow -> planningRow.identifier == planningRowDraft.rowIdentifier }
        }
    }

    fun selectPreviousMonth() {
        mutableSelectedMonth.update(YearMonth::minusMonth)
    }

    fun selectNextMonth() {
        mutableSelectedMonth.update(YearMonth::plusMonth)
    }

    fun openPlanningRowCreation() {
        openPlanningRowDialog(planningRowEditor, planningRow = null)
    }

    fun openPlanningRowEditing(planningRow: FinancialPlanningRow) {
        openPlanningRowDialog(planningRowEditor, planningRow)
    }

    fun openPlanningRowDeletion(planningRow: FinancialPlanningRow) {
        openPlanningRowDialog(planningRowDeletion, planningRow)
    }

    private fun openPlanningRowDialog(
        financialDialogController: FinancialDialogController<FinancialPlanningRowDraft>,
        planningRow: FinancialPlanningRow?,
    ) {
        val planningContent = planning.value.availableContentOrNull() ?: return

        financialDialogController.open(
            FinancialPlanningRowDraft(
                planningTable = planningContent.planningTable,
                month = planningContent.month,
                asset = planningContent.asset,
                rowIdentifier = planningRow?.planningRow?.identifier ?: identifierGenerator.next(),
                category = planningRow?.category,
                plannedAmountText = planningRow
                    ?.let { existingRow -> formatMoneyInput(existingRow.planningRow.plannedAmount, planningContent.asset) }
                    .orEmpty(),
                preferredPaymentMethod = planningRow?.planningRow?.preferredPaymentMethod ?: PaymentMethod.CARD,
            ),
        )
    }

    private fun toPlanningContent(financialSnapshot: FinancialSnapshot): FinancialPlanningContent {
        val planningTableView = (financialSnapshot.planning as? PlanningConfigured)?.table
        val categoriesByIdentifier = financialSnapshot.categories.associateBy(FinancialCategory::identifier)

        return FinancialPlanningContent(
            month = financialSnapshot.period.month,
            asset = financialSnapshot.asset,
            planningTable = planningTableView?.document,
            summary = toPlanningSummary(financialSnapshot.planning, financialSnapshot.asset),
            rows = planningTableView?.rows.orEmpty()
                .map { planningRowView ->
                    FinancialPlanningRow(
                        planningRow = planningRowView.input,
                        category = categoriesByIdentifier[planningRowView.input.categoryIdentifier],
                        categoryName = planningRowView.categoryName,
                        formattedAmount = exactMoneyFormatter.format(planningRowView.input.plannedAmount, financialSnapshot.asset),
                    )
                }
                .toPersistentList(),
            categories = financialSnapshot.categories,
        )
    }

    private fun toPlanningSummary(planningState: PlanningState, asset: AssetDefinition): FinancialPlanningSummary =
        when (planningState) {
            is PlanningConfigured -> toPlanningSummary(
                asset = asset,
                availableAmount = planningState.table.totals.fundsAvailableThisMonth,
                remainingAmount = planningState.table.totals.currentAvailable,
                savableAmount = planningState.table.totals.projectedAvailableAfterPlanning,
            )

            is PlanningNotConfigured -> toPlanningSummary(
                asset = asset,
                availableAmount = planningState.income,
                remainingAmount = planningState.currentAvailable,
                savableAmount = planningState.currentAvailable,
            )
        }

    private fun toPlanningSummary(
        asset: AssetDefinition,
        availableAmount: Money,
        remainingAmount: Money,
        savableAmount: Money,
    ): FinancialPlanningSummary = FinancialPlanningSummary(
        formattedAvailableAmount = exactMoneyFormatter.format(availableAmount, asset),
        formattedRemainingAmount = exactMoneyFormatter.format(remainingAmount, asset),
        formattedSavableAmount = exactMoneyFormatter.format(savableAmount, asset),
    )

    private fun toPlanningRow(planningRowDraft: FinancialPlanningRowDraft): PlanningRow =
        PlanningRow(
            identifier = planningRowDraft.rowIdentifier,
            categoryIdentifier = checkNotNull(planningRowDraft.category).identifier,
            plannedAmount = checkNotNull(planningRowDraft.plannedAmount),
            included = true,
            preferredPaymentMethod = planningRowDraft.preferredPaymentMethod,
        )

    private suspend fun savePlanningRows(
        planningRowDraft: FinancialPlanningRowDraft,
        planningRowsTransformation: (PersistentList<PlanningRow>) -> PersistentList<PlanningRow>,
    ): MutationResult<PlanningTableView> {
        val planningTableInput = planningRowDraft.planningTable?.toInput() ?: FinancialPlanningTableInput(
            identifier = identifierGenerator.next(),
            month = planningRowDraft.month,
            assetIdentifier = planningRowDraft.asset.identifier,
            openingAvailable = Money(planningRowDraft.asset.identifier, 0),
            savingsPolicy = null,
            rows = persistentListOf(),
        )

        return financialGateway.savePlanningTable(
            SavePlanningTable(
                meta = identifierGenerator.nextCommandMeta(),
                expectedVersion = planningRowDraft.planningTable?.version,
                table = planningTableInput.copy(rows = planningRowsTransformation(planningTableInput.rows)),
            ),
        )
    }

    private fun PersistentList<PlanningRow>.withPlanningRow(planningRow: PlanningRow): PersistentList<PlanningRow> {
        val existingRowIndex = indexOfFirst { existingRow -> existingRow.identifier == planningRow.identifier }

        return if (existingRowIndex == -1) {
            adding(planningRow)
        } else {
            replacingAt(existingRowIndex, planningRow)
        }
    }
}
