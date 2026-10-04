package org.orev.nahidka.ui.dashboard

import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.feature.financial.calculation.checkedAdd
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.ui.common.state.StateHolder

@Inject
class DashboardViewModel(
    private val financialGateway: FinancialGateway,
    private val config: FinancialSessionConfig,
    private val clock: ApplicationClock,
    private val moneyFormatter: ExactMoneyFormatter,
) : StateHolder<DashboardState, DashboardEvent>() {
    private val _state = MutableStateFlow(DashboardState())
    override val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val localDate = clock.now().toLocalDateTime(TimeZone.of(config.reportingTimeZone)).date
                val month = YearMonth(localDate.year, localDate.month.ordinal + 1)
                financialGateway.observeFinancialSnapshot(MonthlyQuery(month, config.defaultAssetIdentifier)).collect { snapshot ->
                    var income = 0L
                    snapshot.operations.filter { it.kind == OperationKind.INCOME }.forEach { income = checkedAdd(income, it.amount.units) }
                    val plan = snapshot.planning as? PlanningConfigured
                    val available = plan?.table?.totals?.projectedAvailableAfterPlanning
                    _state.update {
                        it.copy(
                            financialOverview = FinancialOverviewUi(
                                formattedSpent = moneyFormatter.format(snapshot.spending.netExpense, snapshot.asset),
                                formattedIncome = moneyFormatter.format(Money(snapshot.asset.identifier, income), snapshot.asset),
                                formattedAvailableAfterPlanning = available?.let { amount -> moneyFormatter.format(amount, snapshot.asset) },
                                spending = snapshot.spending,
                                assetDisplayCode = snapshot.asset.displayCode,
                                spendingSlices = snapshot.spending.slices.map { slice ->
                                    FinancialSpendingSliceUi(
                                        categoryIdentifier = slice.categoryIdentifier,
                                        label = slice.label,
                                        formattedAmount = moneyFormatter.format(slice.amount, snapshot.asset),
                                        percentageBasisPoints = slice.percentageBasisPoints,
                                        isOtherGroup = slice.isOtherGroup,
                                    )
                                }.toPersistentList(),
                                formattedRefundCredits = moneyFormatter.format(snapshot.spending.refundCredits, snapshot.asset),
                                planningConfigured = plan != null,
                            ),
                            financialError = null,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(financialError = failure.message ?: "Could not load financial summary") }
            }
        }
    }

    override fun handleEvent(event: DashboardEvent) {
        _state.update { state ->
            when (event) {
                is DashboardEvent.TogglePromise -> state.copy(promises = state.promises.map { if (it.identifier == event.identifier) it.copy(completed = !it.completed) else it })
                is DashboardEvent.SetBattery -> state.copy(socialBatteryLevel = event.value.coerceIn(0f, 1f))
                is DashboardEvent.AddEntry -> if (event.text.isBlank()) state else state.copy(
                    promises = if (event.kind == "Add Promise") state.promises + DashboardPromise((state.promises.maxOfOrNull { it.identifier } ?: 0) + 1, event.text, "No due date", "Medium") else state.promises,
                    loveNote = if (event.kind == "New Note") event.text else state.loveNote,
                    activity = listOf("${event.kind}: ${event.text}") + state.activity
                )
            }
        }
    }
}
