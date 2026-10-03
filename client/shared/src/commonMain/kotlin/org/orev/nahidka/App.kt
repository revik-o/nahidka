package org.orev.nahidka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.persistentListOf
import org.orev.nahidka.di.rememberFinancialSession
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.ui.common.theme.NahidkaBackground
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.dashboard.DashboardScreen
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementEvent
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.financialmanagement.FinancialManagementViewModel

@Preview
@Composable
fun App(titleBar: (@Composable () -> Unit)? = null, onFirstFrame: (() -> Unit)? = null) {
    val sessionConfig = remember {
        FinancialSessionConfig(
            sessionIdentity = "demo-session",
            workspaceIdentity = "demo-workspace",
            assets = persistentListOf(AssetDefinition("iso4217:USD", "USD", 2)),
            defaultAssetId = "iso4217:USD",
            reportingTimeZone = "Europe/Kyiv",
        )
    }

    val session = rememberFinancialSession(sessionConfig)
    var showingFinance by remember {
        mutableStateOf(false)
    }

    val dashboardViewModel = viewModel<DashboardViewModel>(
        viewModelStoreOwner = session,
        key = "dashboard",
    ) {
        session.graph.dashboardViewModel
    }

    val financialManagementViewModel = viewModel<FinancialManagementViewModel>(
        viewModelStoreOwner = session,
        key = "financial-management",
    ) {
        session.graph.financialManagementViewModel
    }


    NahidkaTheme {
        Column(Modifier.background(NahidkaBackground).fillMaxSize()) {
            titleBar?.invoke()
            Box(Modifier.safeContentPadding().fillMaxSize().onFirstFrame(onFirstFrame)) {
                if (showingFinance) {
                    FinancialManagementScreen(financialManagementViewModel, onBack = { showingFinance = false })
                } else {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onOpenFinance = {
                            showingFinance = true
                        },
                        onAddExpense = {
                            showingFinance = true
                            financialManagementViewModel.handleEvent(FinancialManagementEvent.OpenAddOperation)
                        },
                    )
                }
            }
        }
    }
}
