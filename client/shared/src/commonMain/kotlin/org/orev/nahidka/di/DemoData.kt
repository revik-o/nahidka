package org.orev.nahidka.di

import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.ui.goal.mock.GoalsMockData
import org.orev.nahidka.ui.tasks.mock.TasksMockData

internal suspend fun populateDemoData(applicationSessionGraph: ApplicationSessionGraph) {
    FinancialDemoData.populate(applicationSessionGraph.financialGateway, applicationSessionGraph.applicationClock)
    TasksMockData.seed(applicationSessionGraph.tasksManager)
    GoalsMockData.seed(applicationSessionGraph.goalsManager)
}
