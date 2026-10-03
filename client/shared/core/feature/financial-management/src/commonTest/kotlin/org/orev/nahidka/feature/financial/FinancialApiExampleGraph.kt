package org.orev.nahidka.feature.financial

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

@DependencyGraph(FinancialSessionScope::class)
interface FinancialApiExampleGraph {
    val finance: FinancialModule

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides gateway: FinancialGateway): FinancialApiExampleGraph
    }
}
