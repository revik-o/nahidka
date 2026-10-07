package org.orev.nahidka

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.orev.nahidka.core.common.SystemApplicationClock
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.di.rememberFinancialSession
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsManager
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.dashboard.DashboardScreen
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.goal.GoalsScreen
import org.orev.nahidka.ui.goal.GoalsScreenGraph
import org.orev.nahidka.ui.goal.GoalsViewModel
import org.orev.nahidka.ui.goal.mock.GoalsMockData
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.socialbattery.SocialBatteryScreen
import org.orev.nahidka.ui.socialbattery.SocialBatteryScreenGraph
import org.orev.nahidka.ui.socialbattery.SocialBatteryViewModel
import org.orev.nahidka.ui.tasks.TasksScreen
import org.orev.nahidka.ui.tasks.TasksScreenGraph
import org.orev.nahidka.ui.tasks.TasksViewModel
import org.orev.nahidka.ui.tasks.mock.TasksMockData

@Preview
@Composable
fun App(
    titleBar: (@Composable () -> Unit)? = null,
    onFirstFrame: (() -> Unit)? = null,
    settingsStorage: SettingsLocalDataSource? = null,
) {
    val settingsLocalDataSource = settingsStorage ?: rememberSettingsLocalDataSource()
    val settingsContext = remember(settingsLocalDataSource) { SettingsContext(settingsLocalDataSource) }
    val settingsManager = remember(settingsContext) { SettingsManager(settingsContext) }
    val settingsSnapshot by settingsContext.settingsState.collectAsState()
    val settingsScope = rememberCoroutineScope()
    var showingSettings by remember { mutableStateOf(false) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    val darkTheme = when (settingsSnapshot.settings.theme) {
        SettingsTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        SettingsTheme.LIGHT -> false
        SettingsTheme.DARK -> true
    }
    val sessionConfig = FinancialDemoData.sessionConfig

    val session = rememberFinancialSession(sessionConfig)
    LaunchedEffect(session) {
        FinancialDemoData.populate(session.graph.gateway, SystemApplicationClock())
    }
    val tasksScreenGraph = remember(session) { createGraph<TasksScreenGraph>() }
    LaunchedEffect(tasksScreenGraph) {
        TasksMockData.seed(tasksScreenGraph.tasksManager)
    }
    val goalsScreenGraph = remember(session) { createGraph<GoalsScreenGraph>() }
    LaunchedEffect(goalsScreenGraph) {
        GoalsMockData.seed(goalsScreenGraph.goalsManager)
    }
    val socialBatteryScreenGraph = remember(session) { createGraph<SocialBatteryScreenGraph>() }
    var showingFinance by remember {
        mutableStateOf(false)
    }
    var showingTasks by remember { mutableStateOf(false) }
    var showingGoals by remember { mutableStateOf(false) }
    var showingSocialBattery by remember { mutableStateOf(false) }

    val dashboardViewModel = viewModel<DashboardViewModel>(
        viewModelStoreOwner = session,
        key = "dashboard",
    ) {
        session.graph.dashboardViewModel
    }

    val tasksViewModel = viewModel<TasksViewModel>(
        viewModelStoreOwner = session,
        key = "tasks",
    ) {
        tasksScreenGraph.tasksViewModel
    }

    val goalsViewModel = viewModel<GoalsViewModel>(
        viewModelStoreOwner = session,
        key = "goals",
    ) {
        goalsScreenGraph.goalsViewModel
    }

    val socialBatteryViewModel = viewModel<SocialBatteryViewModel>(
        viewModelStoreOwner = session,
        key = "social-battery",
    ) {
        socialBatteryScreenGraph.socialBatteryViewModel
    }

    val financialHistoryViewModel = viewModel<FinancialHistoryViewModel>(
        viewModelStoreOwner = session,
        key = "financial-history",
    ) {
        session.graph.financialHistoryViewModel
    }

    val financialPlanningViewModel = viewModel<FinancialPlanningViewModel>(
        viewModelStoreOwner = session,
        key = "financial-planning",
    ) {
        session.graph.financialPlanningViewModel
    }


    NahidkaTheme(darkTheme = darkTheme) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()) {
            titleBar?.invoke()
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { showingSettings = false; showingFinance = false; showingTasks = false; showingGoals = false; showingSocialBattery = false }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true }) { Text("Settings") }
                TextButton(onClick = { showingSettings = false; showingTasks = true }) { Text("Tasks") }
                TextButton(onClick = { showingSettings = false; showingTasks = false; showingGoals = true }) { Text("Goals") }
                TextButton(onClick = { showingSettings = false; showingTasks = false; showingGoals = false; showingSocialBattery = true }) { Text("Social battery") }
            }
            Box(Modifier.safeContentPadding().fillMaxSize().onFirstFrame(onFirstFrame)) {
                if (showingSettings) {
                    SettingsScreen(settingsSnapshot.settings, onSettingsChange = { settings ->
                        settingsScope.launch {
                            try {
                                settingsManager.saveSettings(settings)
                                settingsError = null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                settingsError = failure.message ?: "Could not save settings"
                            }
                        }
                    }, errorMessage = settingsError)
                } else if (showingTasks) {
                    TasksScreen(tasksViewModel)
                } else if (showingGoals) {
                    GoalsScreen(goalsViewModel)
                } else if (showingSocialBattery) {
                    SocialBatteryScreen(socialBatteryViewModel)
                } else if (showingFinance) {
                    FinancialManagementScreen(financialHistoryViewModel, financialPlanningViewModel)
                } else {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onOpenFinance = {
                            showingFinance = true
                        },
                        onAddExpense = {
                            showingFinance = true
                            financialHistoryViewModel.openOperationCreation()
                        },
                    )
                }
            }
        }
    }
}
