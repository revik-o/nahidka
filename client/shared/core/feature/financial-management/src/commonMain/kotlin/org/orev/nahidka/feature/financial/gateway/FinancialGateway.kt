package org.orev.nahidka.feature.financial.gateway

import kotlinx.coroutines.flow.Flow
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.dto.*
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
