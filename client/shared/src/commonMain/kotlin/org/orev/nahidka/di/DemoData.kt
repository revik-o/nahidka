package org.orev.nahidka.di

import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.ui.goal.mock.GoalsMockData
import org.orev.nahidka.ui.tasks.mock.TasksMockData
import org.orev.nahidka.ui.dashboard.PartnerPresence

internal suspend fun populateDemoData(applicationSessionGraph: ApplicationSessionGraph) {
    applicationSessionGraph.partnerPresenceStore.replace(
        listOf(
            PartnerPresence("demo-partner-alex", "Alex", "A", isOnline = true),
            PartnerPresence("demo-partner-sam", "Sam", "S", isOnline = true),
        ),
    )
    FinancialDemoData.populate(applicationSessionGraph.financialGateway, applicationSessionGraph.applicationClock)
    TasksMockData.seed(applicationSessionGraph.tasksManager)
    GoalsMockData.seed(applicationSessionGraph.goalsManager)
}
