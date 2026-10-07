package org.orev.nahidka.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.core.common.*
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel

@DependencyGraph(FinancialSessionScope::class, bindingContainers = [IdentifierGeneratorBindings::class])
interface FinancialSessionGraph {
    val finance: FinancialModule
    val gateway: FinancialGateway
    val dashboardViewModel: DashboardViewModel
    val financialHistoryViewModel: FinancialHistoryViewModel
    val financialPlanningViewModel: FinancialPlanningViewModel

    @Provides
    @SingleIn(FinancialSessionScope::class)
    fun provideGateway(
        config: FinancialSessionConfig,
        errorReporter: ErrorReporter,
        calendar: FinancialCalendar,
    ): FinancialGateway = InMemoryFinancialGateway(config, errorReporter, calendar)

    @Provides
    fun provideErrorReporter(): ErrorReporter = NoOpErrorReporter()

    @Provides
    fun provideApplicationClock(): ApplicationClock = SystemApplicationClock()

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides config: FinancialSessionConfig): FinancialSessionGraph
    }
}
