package org.orev.nahidka.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.feature.financial.support.FinancialClock
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter
import org.orev.nahidka.feature.financial.support.FinancialIdGenerator
import org.orev.nahidka.feature.financial.support.NoOpFinancialErrorReporter
import org.orev.nahidka.feature.financial.support.RandomFinancialIdGenerator
import org.orev.nahidka.feature.financial.support.SystemFinancialClock
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementViewModel

@DependencyGraph(FinancialSessionScope::class)
interface FinancialSessionGraph {
    val finance: FinancialModule
    val gateway: FinancialGateway
    val dashboardViewModel: DashboardViewModel
    val financialManagementViewModel: FinancialManagementViewModel

    @Provides
    @SingleIn(FinancialSessionScope::class)
    fun provideGateway(
        config: FinancialSessionConfig,
        errorReporter: FinancialErrorReporter,
        calendar: FinancialCalendar,
    ): FinancialGateway = InMemoryFinancialGateway(config, errorReporter, calendar)

    @Provides
    fun provideFinancialErrorReporter(): FinancialErrorReporter = NoOpFinancialErrorReporter()

    @Provides
    fun provideFinancialClock(): FinancialClock = SystemFinancialClock()

    @Provides
    fun provideFinancialIdGenerator(): FinancialIdGenerator = RandomFinancialIdGenerator()

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides config: FinancialSessionConfig): FinancialSessionGraph
    }
}
