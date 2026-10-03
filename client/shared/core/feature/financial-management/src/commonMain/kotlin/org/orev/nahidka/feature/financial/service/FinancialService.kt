package org.orev.nahidka.feature.financial.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

@SingleIn(FinancialSessionScope::class)
class FinancialService @Inject constructor(private val gateway: FinancialGateway) {
    suspend fun addNewFinancialManipulation(command: AddFinancialOperation): MutationResult<FinancialOperation> =
        gateway.addOperation(command)

    suspend fun removeFinancialManipulation(command: RemoveFinancialOperation): MutationResult<FinancialOperation> =
        gateway.removeOperation(command)

    suspend fun updateFinancialManipulation(command: UpdateFinancialOperation): MutationResult<FinancialOperation> =
        gateway.updateOperation(command)
}
