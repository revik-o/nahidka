package org.orev.nahidka.feature.financial.store

import kotlin.coroutines.coroutineContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.feature.financial.calculation.checkedNextVersion
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.subscription.EntityDelta
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter
import org.orev.nahidka.feature.financial.support.FinancialOverflowException

internal class FinancialStore(
    private val config: FinancialSessionConfig,
    private val reducer: FinancialReducer,
    private val errorReporter: FinancialErrorReporter,
) {
    private val mutex = Mutex()
    private val initialData = FinancialData(
        operations = persistentMapOf(),
        categories = persistentMapOf(),
        planningTables = persistentMapOf(),
        usedOperationIds = persistentSetOf(),
        usedCategoryIds = persistentSetOf(),
        usedPlanningTableIds = persistentSetOf(),
        usedPlanningRowIds = persistentSetOf(),
    )
    private val initialFence = CompletableDeferred(Unit)
    private val mutableFrames = MutableStateFlow(
        InternalFrame(
            sessionIdentity = config.sessionIdentity,
            revision = 0,
            data = initialData,
            journal = persistentListOf(),
            closed = false,
            released = initialFence,
        ),
    )
    val frames: StateFlow<InternalFrame> = mutableFrames.asStateFlow()
    val sessionLifetime: Job = Job()
    val reporter: FinancialErrorReporter = errorReporter
    private var retainedReceipts = persistentMapOf<String, RetainedCommandReceipt>()
    private var receiptOrder = persistentListOf<String>()

    suspend fun execute(command: FinancialCommand): MutationResult<CommandReceipt> {
        val callerContext = coroutineContext
        val completed = mutex.withLock {
            callerContext.ensureActive()
            val old = mutableFrames.value
            if (old.closed) return@withLock CompletedCommand(MutationResult.Rejected(FinancialError.SessionClosed), null)
            val commandId = command.meta.commandId
            if (commandId.isBlank() || commandId.length > 128) {
                return@withLock CompletedCommand(
                    MutationResult.Rejected(FinancialError.Validation("meta.commandId", "A valid command ID is required")),
                    null,
                )
            }
            val retained = retainedReceipts[commandId]
            if (retained != null) {
                return@withLock if (retained.command == command) {
                    val receipt = retained.receipt
                    CompletedCommand(MutationResult.Committed(receipt, receipt.storeRevision, receipt.changed), null)
                } else {
                    CompletedCommand(MutationResult.Rejected(FinancialError.CommandIdReused(commandId)), null)
                }
            }
            when (val reduction = reducer.reduce(old.data, old.revision, command)) {
                is FinancialReduction.Rejected -> CompletedCommand(MutationResult.Rejected(reduction.error), null)
                is FinancialReduction.Accepted -> {
                    if (!reduction.changed) {
                        val receipt = CommandReceipt(reduction.value, old.revision, false)
                        val retainedState = retain(commandId, command, receipt)
                        retainedReceipts = retainedState.first
                        receiptOrder = retainedState.second
                        CompletedCommand(MutationResult.Committed(receipt, old.revision, false), null)
                    } else {
                        val nextRevision = try {
                            checkedNextVersion(old.revision)
                        } catch (_: FinancialOverflowException) {
                            return@withLock CompletedCommand(
                                MutationResult.Rejected(FinancialError.Validation("revision", "The session revision limit has been reached")),
                                null,
                            )
                        }
                        val receipt = CommandReceipt(reduction.value, nextRevision, true)
                        val commit = makeCommit(command, old.data, reduction.data, nextRevision)
                        val nextJournal = (old.journal + commit).takeLastPersistent(config.journalCapacity)
                        val released = CompletableDeferred<Unit>()
                        val nextFrame = InternalFrame(
                            sessionIdentity = old.sessionIdentity,
                            revision = nextRevision,
                            data = reduction.data,
                            journal = nextJournal,
                            closed = false,
                            released = released,
                        )
                        val retainedState = retain(commandId, command, receipt)
                        retainedReceipts = retainedState.first
                        receiptOrder = retainedState.second
                        mutableFrames.value = nextFrame
                        CompletedCommand(MutationResult.Committed(receipt, nextRevision, true), released)
                    }
                }
            }
        }
        completed.released?.complete(Unit)
        return completed.result
    }

    suspend fun close() {
        val released = mutex.withLock {
            val old = mutableFrames.value
            if (old.closed) return@withLock null
            val nextRevision = if (old.revision == Long.MAX_VALUE) old.revision else old.revision + 1
            val fence = CompletableDeferred<Unit>()
            val empty = initialData
            mutableFrames.value = InternalFrame(
                sessionIdentity = old.sessionIdentity,
                revision = nextRevision,
                data = empty,
                journal = persistentListOf(),
                closed = true,
                released = fence,
            )
            retainedReceipts = persistentMapOf()
            receiptOrder = persistentListOf()
            fence
        }
        released?.complete(Unit)
        sessionLifetime.cancel(CancellationException("Financial session closed"))
    }

    private fun retain(
        commandId: String,
        command: FinancialCommand,
        receipt: CommandReceipt,
    ): Pair<kotlinx.collections.immutable.PersistentMap<String, RetainedCommandReceipt>, kotlinx.collections.immutable.PersistentList<String>> {
        var nextMap = retainedReceipts.putting(commandId, RetainedCommandReceipt(command, receipt))
        var nextOrder = receiptOrder.adding(commandId)
        while (nextOrder.size > config.commandReceiptCapacity) {
            val expired = nextOrder.first()
            nextMap = nextMap.removing(expired)
            nextOrder = nextOrder.removingAt(0)
        }
        return nextMap to nextOrder
    }

    private fun makeCommit(
        command: FinancialCommand,
        before: FinancialData,
        after: FinancialData,
        revision: Long,
    ): FinancialCommit {
        var operations = persistentListOf<EntityDelta<FinancialOperation>>()
        var categories = persistentListOf<EntityDelta<FinancialCategory>>()
        var tables = persistentListOf<EntityDelta<FinancialPlanningTable>>()
        when (command) {
            is FinancialCommand.AddOperation -> {
                val id = command.command.operation.id
                operations = operations.adding(EntityDelta(before.operations[id], after.operations[id]))
            }
            is FinancialCommand.UpdateOperation -> {
                val id = command.command.id
                operations = operations.adding(EntityDelta(before.operations[id], after.operations[id]))
            }
            is FinancialCommand.RemoveOperation -> {
                val id = command.command.id
                operations = operations.adding(EntityDelta(before.operations[id], after.operations[id]))
            }
            is FinancialCommand.SavePlanning -> {
                val id = command.command.table.id
                tables = tables.adding(EntityDelta(before.planningTables[id], after.planningTables[id]))
            }
            is FinancialCommand.DeletePlanning -> {
                val id = command.command.id
                tables = tables.adding(EntityDelta(before.planningTables[id], after.planningTables[id]))
            }
            is FinancialCommand.CreateCategory -> {
                val id = command.command.id
                categories = categories.adding(EntityDelta(before.categories[id], after.categories[id]))
            }
            is FinancialCommand.UpdateCategory -> {
                val id = command.command.id
                categories = categories.adding(EntityDelta(before.categories[id], after.categories[id]))
            }
            is FinancialCommand.ArchiveCategory -> {
                val id = command.command.id
                categories = categories.adding(EntityDelta(before.categories[id], after.categories[id]))
            }
            is FinancialCommand.DeleteCategory -> {
                val id = command.command.id
                categories = categories.adding(EntityDelta(before.categories[id], after.categories[id]))
            }
        }
        return FinancialCommit(revision, command.meta.commandId, before, after, operations, categories, tables)
    }
}

private fun <T> List<T>.takeLastPersistent(limit: Int) =
    takeLast(limit).toPersistentList()
