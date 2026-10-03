package org.orev.nahidka.feature.financial.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.command.DeletePlanningTable
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

@SingleIn(FinancialSessionScope::class)
class FinancialPlanningService @Inject constructor(private val gateway: FinancialGateway) {
    suspend fun saveTable(command: SavePlanningTable): MutationResult<PlanningTableView> =
        gateway.savePlanningTable(command)

    suspend fun deleteTable(command: DeletePlanningTable): MutationResult<PlanningTableView> =
        gateway.deletePlanningTable(command)
}
