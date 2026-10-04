package org.orev.nahidka.feature.financial.gateway

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.orev.nahidka.core.common.ErrorReporter
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.calculation.calculateFinancialSnapshot
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.*
import org.orev.nahidka.feature.financial.subscription.*

class InMemoryFinancialGateway @Inject constructor(
    private val config: FinancialSessionConfig,
    private val errorReporter: ErrorReporter,
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
        FinancialSubscription(
            PlanningWatchSource(
                store.frames,
                store.sessionLifetime,
                errorReporter,
                query,
                config,
                calendar
            )
        )

    override fun subscribeCategories(query: CategoryQuery): FinancialSubscription<FinancialCategory> =
        FinancialSubscription(CategoryWatchSource(store.frames, store.sessionLifetime, errorReporter, query))

    override fun observeFinancialSnapshot(query: MonthlyQuery): Flow<FinancialSnapshot> =
        store.frames.transform { frame ->
            frame.awaitRelease()

            if (frame.closed) {
                throw FinancialSessionClosedException()
            }

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
