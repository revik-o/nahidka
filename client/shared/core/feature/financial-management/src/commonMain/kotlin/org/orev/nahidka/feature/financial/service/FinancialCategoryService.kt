package org.orev.nahidka.feature.financial.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

@SingleIn(FinancialSessionScope::class)
class FinancialCategoryService @Inject constructor(private val gateway: FinancialGateway) {
    suspend fun createCategory(command: CreateFinancialCategory): MutationResult<FinancialCategory> =
        gateway.createCategory(command)

    suspend fun updateCategory(command: UpdateFinancialCategory): MutationResult<FinancialCategory> =
        gateway.updateCategory(command)

    suspend fun archiveCategory(command: ArchiveFinancialCategory): MutationResult<FinancialCategory> =
        gateway.archiveCategory(command)

    suspend fun deleteCategory(command: DeleteFinancialCategory): MutationResult<FinancialCategory> =
        gateway.deleteCategory(command)
}
