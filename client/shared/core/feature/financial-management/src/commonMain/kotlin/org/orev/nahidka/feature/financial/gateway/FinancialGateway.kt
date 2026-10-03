package org.orev.nahidka.feature.financial.gateway

import kotlinx.coroutines.flow.Flow
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.DeletePlanningTable
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.dto.CategoryQuery
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialSnapshot
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.dto.PlanningQuery
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription

interface FinancialGateway {
    suspend fun addOperation(command: AddFinancialOperation): MutationResult<FinancialOperation>
    suspend fun updateOperation(command: UpdateFinancialOperation): MutationResult<FinancialOperation>
    suspend fun removeOperation(command: RemoveFinancialOperation): MutationResult<FinancialOperation>
    suspend fun savePlanningTable(command: SavePlanningTable): MutationResult<PlanningTableView>
    suspend fun deletePlanningTable(command: DeletePlanningTable): MutationResult<PlanningTableView>
    suspend fun createCategory(command: CreateFinancialCategory): MutationResult<FinancialCategory>
    suspend fun updateCategory(command: UpdateFinancialCategory): MutationResult<FinancialCategory>
    suspend fun archiveCategory(command: ArchiveFinancialCategory): MutationResult<FinancialCategory>
    suspend fun deleteCategory(command: DeleteFinancialCategory): MutationResult<FinancialCategory>
    fun subscribeOperations(query: OperationQuery): FinancialSubscription<FinancialOperation>
    fun subscribePlanning(query: PlanningQuery): FinancialSubscription<PlanningTableView>
    fun subscribeCategories(query: CategoryQuery): FinancialSubscription<FinancialCategory>
    fun observeFinancialSnapshot(query: MonthlyQuery): Flow<FinancialSnapshot>
    suspend fun close()
}
