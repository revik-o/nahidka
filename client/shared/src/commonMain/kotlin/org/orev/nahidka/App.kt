package org.orev.nahidka

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsManager
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.settings.SettingsScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.persistentListOf
import org.orev.nahidka.api.TaskStatus
import org.orev.nahidka.di.rememberFinancialSession
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.dashboard.DashboardScreen
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementEvent
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.financialmanagement.FinancialManagementViewModel

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
    val sessionConfig = remember {
        FinancialSessionConfig(
            sessionIdentity = "demo-session",
            workspaceIdentity = "demo-workspace",
            assets = persistentListOf(AssetDefinition("iso4217:USD", "USD", 2)),
            defaultAssetIdentifier = "iso4217:USD",
            reportingTimeZone = "Europe/Kyiv",
        )
    }

    val session = rememberFinancialSession(sessionConfig)
    val tasksContext = remember(session) { TasksContext() }
    val tasksManager = remember(tasksContext) { TasksManager(tasksContext) }
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
    val batteryContext = remember(session) { SocialBatteryContext() }
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val tasksSnapshot by tasksContext.tasksState.collectAsState()
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


    NahidkaTheme(darkTheme = darkTheme) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()) {
            titleBar?.invoke()
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { showingSettings = false; showingFinance = false; selectedPersonalFeature = null }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true; selectedPersonalFeature = null }) { Text("Settings") }
                PersonalFeature.entries.forEach { feature ->
                    TextButton(onClick = { showingSettings = false; selectedPersonalFeature = feature }) {
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
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        tasks = tasksSnapshot.tasks,
                        goals = goalsSnapshot.goals,
                        batteryPercentage = batterySnapshot.socialBattery?.percentage,
                        onCreateTask = { title -> mutatePersonalFeature { tasksManager.createTasks(listOf(TaskCreationRequest(identifierGenerator.next(), title))) } },
                        onUpdateTask = { task -> mutatePersonalFeature {
                            val nextStatus = TaskStatus.entries[(task.status.ordinal + 1) % TaskStatus.entries.size]
                            tasksManager.updateTasks(listOf(TaskUpdateRequest(task.identifier, status = nextStatus)))
                        } },
                        onCreateGoal = { title -> mutatePersonalFeature { goalsManager.createGoal(GoalCreationRequest(identifierGenerator.next(), title)) } },
                        onUpdateGoal = { goal -> mutatePersonalFeature {
                            goalsManager.updateGoal(GoalUpdateRequest(goal.identifier, progressPercentage = (goal.progressPercentage + 10f).coerceAtMost(100f)))
                        } },
                        onUpdateBattery = { percentage -> mutatePersonalFeature { batteryManager.updateBattery(SocialBattery(percentage)) } },
                        errorMessage = personalFeatureError,
                    )
                } else if (showingFinance) {
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
