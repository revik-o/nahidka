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
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.core.common.SystemApplicationClock
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.di.rememberFinancialSession
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsManager
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.dashboard.DashboardScreen
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.settings.SettingsScreen
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
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
    val batteryContext = remember(session) { SocialBatteryContext() }
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val goalsSnapshot by goalsContext.goalsState.collectAsState()
    val batterySnapshot by batteryContext.batteryState.collectAsState()
    var selectedPersonalFeature by remember { mutableStateOf<PersonalFeature?>(null) }
    var personalFeatureError by remember { mutableStateOf<String?>(null) }
    fun mutatePersonalFeature(mutation: suspend () -> Unit) {
        settingsScope.launch {
            try {
                mutation()
                personalFeatureError = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                personalFeatureError = failure.message ?: "Could not save this change"
            }
        }
    }
    var showingFinance by remember {
        mutableStateOf(false)
    }
    var showingTasks by remember { mutableStateOf(false) }

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
                TextButton(onClick = { showingSettings = false; showingFinance = false; showingTasks = false; selectedPersonalFeature = null }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true; selectedPersonalFeature = null }) { Text("Settings") }
                TextButton(onClick = { showingSettings = false; showingTasks = true; selectedPersonalFeature = null }) { Text("Tasks") }
                PersonalFeature.entries.forEach { feature ->
                    TextButton(onClick = { showingSettings = false; showingTasks = false; selectedPersonalFeature = feature }) {
                        Text(feature.name.lowercase().replace('_', ' '))
                    }
                }
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
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        goals = goalsSnapshot.goals,
                        batteryPercentage = batterySnapshot.socialBattery?.percentage,
                        onCreateGoal = { title -> mutatePersonalFeature { goalsManager.createGoal(GoalCreationRequest(identifierGenerator.next(), title)) } },
                        onUpdateGoal = { goal -> mutatePersonalFeature {
                            goalsManager.updateGoal(GoalUpdateRequest(goal.identifier, progressPercentage = (goal.progressPercentage + 10f).coerceAtMost(100f)))
                        } },
                        onUpdateBattery = { percentage -> mutatePersonalFeature { batteryManager.updateBattery(SocialBattery(percentage)) } },
                        errorMessage = personalFeatureError,
                    )
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
