package org.orev.nahidka.feature.financial.gateway

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.calculation.calculateFinancialSnapshot
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
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.FinancialSnapshot
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.dto.PlanningQuery
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.store.FinancialCommand
import org.orev.nahidka.feature.financial.store.FinancialReducer
import org.orev.nahidka.feature.financial.store.FinancialStore
import org.orev.nahidka.feature.financial.store.FinancialValue
import org.orev.nahidka.feature.financial.subscription.CategoryWatchSource
import org.orev.nahidka.feature.financial.subscription.FinancialSessionClosedException
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription
import org.orev.nahidka.feature.financial.subscription.OperationWatchSource
import org.orev.nahidka.feature.financial.subscription.PlanningWatchSource
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter

class InMemoryFinancialGateway @Inject constructor(
    private val config: FinancialSessionConfig,
    private val errorReporter: FinancialErrorReporter,
    private val calendar: FinancialCalendar,
) : FinancialGateway {
    private val store = FinancialStore(config, FinancialReducer(config, calendar), errorReporter)

    override suspend fun addOperation(command: AddFinancialOperation): MutationResult<FinancialOperation> =
        execute(FinancialCommand.AddOperation(command)) { (it as? FinancialValue.Operation)?.value }

    override suspend fun updateOperation(command: UpdateFinancialOperation): MutationResult<FinancialOperation> =
        execute(FinancialCommand.UpdateOperation(command)) { (it as? FinancialValue.Operation)?.value }

    override suspend fun removeOperation(command: RemoveFinancialOperation): MutationResult<FinancialOperation> =
        execute(FinancialCommand.RemoveOperation(command)) { (it as? FinancialValue.Operation)?.value }

    override suspend fun savePlanningTable(command: SavePlanningTable): MutationResult<PlanningTableView> =
        execute(FinancialCommand.SavePlanning(command)) { (it as? FinancialValue.PlanningTable)?.view }

    override suspend fun deletePlanningTable(command: DeletePlanningTable): MutationResult<PlanningTableView> =
        execute(FinancialCommand.DeletePlanning(command)) { (it as? FinancialValue.PlanningTable)?.view }

    override suspend fun createCategory(command: CreateFinancialCategory): MutationResult<FinancialCategory> =
        execute(FinancialCommand.CreateCategory(command)) { (it as? FinancialValue.Category)?.value }

    override suspend fun updateCategory(command: UpdateFinancialCategory): MutationResult<FinancialCategory> =
        execute(FinancialCommand.UpdateCategory(command)) { (it as? FinancialValue.Category)?.value }

    override suspend fun archiveCategory(command: ArchiveFinancialCategory): MutationResult<FinancialCategory> =
        execute(FinancialCommand.ArchiveCategory(command)) { (it as? FinancialValue.Category)?.value }

    override suspend fun deleteCategory(command: DeleteFinancialCategory): MutationResult<FinancialCategory> =
        execute(FinancialCommand.DeleteCategory(command)) { (it as? FinancialValue.Category)?.value }

    override fun subscribeOperations(query: OperationQuery): FinancialSubscription<FinancialOperation> =
        FinancialSubscription(OperationWatchSource(store.frames, store.sessionLifetime, errorReporter, query))

    override fun subscribePlanning(query: PlanningQuery): FinancialSubscription<PlanningTableView> =
        FinancialSubscription(PlanningWatchSource(store.frames, store.sessionLifetime, errorReporter, query, config, calendar))

    override fun subscribeCategories(query: CategoryQuery): FinancialSubscription<FinancialCategory> =
        FinancialSubscription(CategoryWatchSource(store.frames, store.sessionLifetime, errorReporter, query))

    override fun observeFinancialSnapshot(query: MonthlyQuery): Flow<FinancialSnapshot> =
        store.frames.transform { frame ->
            frame.released.await()
            if (frame.closed) throw FinancialSessionClosedException()
            emit(calculateFinancialSnapshot(frame, query, config, calendar))
        }

    override suspend fun close() = store.close()

    private suspend fun <T> execute(
        command: FinancialCommand,
        decode: (FinancialValue) -> T?,
    ): MutationResult<T> = when (val result = store.execute(command)) {
        is MutationResult.Rejected -> result
        is MutationResult.Committed -> {
            val value = decode(result.value.value)
                ?: error("Financial command receipt type does not match its command")
            MutationResult.Committed(value, result.value.storeRevision, result.value.changed)
        }
    }
}
