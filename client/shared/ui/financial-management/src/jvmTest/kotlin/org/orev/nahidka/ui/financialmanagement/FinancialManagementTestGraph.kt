package org.orev.nahidka.ui.financialmanagement

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.persistentListOf
import org.orev.nahidka.core.common.*
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import kotlin.time.Instant

internal val FINANCIAL_TEST_ASSET = AssetDefinition("iso4217:USD", "USD", 2)

@DependencyGraph(FinancialSessionScope::class)
internal interface FinancialManagementTestGraph {
    val financialGateway: FinancialGateway
    val financialHistoryViewModel: FinancialHistoryViewModel
    val financialPlanningViewModel: FinancialPlanningViewModel

    @Provides
    @SingleIn(FinancialSessionScope::class)
    fun provideFinancialGateway(
        financialSessionConfig: FinancialSessionConfig,
        financialCalendar: FinancialCalendar,
    ): FinancialGateway = InMemoryFinancialGateway(financialSessionConfig, NoOpErrorReporter(), financialCalendar)

    @Provides
    fun provideFinancialSessionConfig(): FinancialSessionConfig = FinancialSessionConfig(
        sessionIdentity = "test-session",
        workspaceIdentity = "test-workspace",
        assets = persistentListOf(FINANCIAL_TEST_ASSET, AssetDefinition("iso4217:UAH", "UAH", 2)),
        defaultAssetIdentifier = FINANCIAL_TEST_ASSET.identifier,
        reportingTimeZone = "Europe/Kyiv",
    )

    @Provides
    fun provideApplicationClock(): ApplicationClock = ApplicationClock { Instant.parse("2026-10-04T12:00:00Z") }

    @Provides
    fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
