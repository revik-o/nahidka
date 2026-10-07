package org.orev.nahidka.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.datetime.TimeZone
import org.orev.nahidka.core.common.*
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.feature.goals.di.GoalsBindings
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.settings.di.SettingsBindings
import org.orev.nahidka.feature.settings.di.SettingsSessionScope
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.socialbattery.di.SocialBatteryBindings
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionScope
import org.orev.nahidka.feature.tasks.di.TasksBindings
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.overview.FinancialOverviewViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.goal.GoalsViewModel
import org.orev.nahidka.ui.notification.NotificationsViewModel
import org.orev.nahidka.ui.settings.SettingsViewModel
import org.orev.nahidka.ui.socialbattery.SocialBatteryViewModel
import org.orev.nahidka.ui.tasks.TasksViewModel

@DependencyGraph(
    scope = ApplicationSessionScope::class,
    additionalScopes = [
        FinancialSessionScope::class,
        TasksSessionScope::class,
        GoalsSessionScope::class,
        SocialBatterySessionScope::class,
        SettingsSessionScope::class,
    ],
    bindingContainers = [
        IdentifierGeneratorBindings::class,
        TasksBindings::class,
        GoalsBindings::class,
        SocialBatteryBindings::class,
        SettingsBindings::class,
    ],
)
interface ApplicationSessionGraph {
    val applicationClock: ApplicationClock
    val financialGateway: FinancialGateway
    val tasksManager: TasksManager
    val goalsManager: GoalsManager
    val dashboardViewModel: DashboardViewModel
    val socialBatteryViewModel: SocialBatteryViewModel
    val tasksViewModel: TasksViewModel
    val goalsViewModel: GoalsViewModel
    val settingsViewModel: SettingsViewModel
    val notificationsViewModel: NotificationsViewModel
    val financialOverviewViewModel: FinancialOverviewViewModel
    val financialHistoryViewModel: FinancialHistoryViewModel
    val financialPlanningViewModel: FinancialPlanningViewModel

    @Provides
    @SingleIn(FinancialSessionScope::class)
    fun provideFinancialGateway(
        financialSessionConfig: FinancialSessionConfig,
        errorReporter: ErrorReporter,
        financialCalendar: FinancialCalendar,
    ): FinancialGateway = InMemoryFinancialGateway(financialSessionConfig, errorReporter, financialCalendar)

    @Provides
    fun provideErrorReporter(): ErrorReporter = NoOpErrorReporter()

    @Provides
    fun provideApplicationClock(): ApplicationClock = SystemApplicationClock()

    @Provides
    fun provideTimeZone(): TimeZone = TimeZone.currentSystemDefault()

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides financialSessionConfig: FinancialSessionConfig,
            @Provides settingsLocalDataSource: SettingsLocalDataSource,
        ): ApplicationSessionGraph
    }
}
