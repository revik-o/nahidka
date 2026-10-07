package org.orev.nahidka.ui.financialmanagement.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.ui.financialmanagement.FinancialAssetSelection
import org.orev.nahidka.ui.financialmanagement.common.FinancialContentState
import org.orev.nahidka.ui.financialmanagement.common.stateInFinancialContent

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class FinancialOverviewViewModel(
    financialSessionConfig: FinancialSessionConfig,
    financialGateway: FinancialGateway,
    financialAssetSelection: FinancialAssetSelection,
    applicationClock: ApplicationClock,
    exactMoneyFormatter: ExactMoneyFormatter,
) : ViewModel() {

    private val reportingMonth = financialMonthFor(applicationClock.now(), financialSessionConfig.reportingTimeZone)

    val overview: StateFlow<FinancialContentState<FinancialOverview>> = financialAssetSelection.selectedAsset
        .flatMapLatest { selectedAsset ->
            financialGateway.observeFinancialSnapshot(MonthlyQuery(reportingMonth, selectedAsset.identifier))
        }
        .map { financialSnapshot -> FinancialOverview.of(financialSnapshot, exactMoneyFormatter) }
        .stateInFinancialContent(viewModelScope)
}
