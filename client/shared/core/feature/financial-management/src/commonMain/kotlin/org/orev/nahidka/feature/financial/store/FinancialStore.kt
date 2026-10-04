package org.orev.nahidka.feature.financial.store

import kotlin.coroutines.coroutineContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
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
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.subscription.EntityDelta
import org.orev.nahidka.core.common.ErrorReporter
import org.orev.nahidka.core.common.ArithmeticOverflowException

internal class FinancialStore(
    private val config: FinancialSessionConfig,
    private val reducer: FinancialReducer,
    private val errorReporter: ErrorReporter,
) {

    private val mutex = Mutex()
    private val initialData = FinancialData(
        operations = persistentMapOf(),
        categories = persistentMapOf(),
        planningTables = persistentMapOf(),
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
    private var retainedReceipts = persistentMapOf<String, RetainedCommandReceipt>()
    private var receiptOrder = persistentListOf<String>()

    val frames: StateFlow<InternalFrame> = mutableFrames.asStateFlow()
    val sessionLifetime: Job = Job()
    val reporter: ErrorReporter = errorReporter

    suspend fun execute(command: FinancialCommand): MutationResult<CommandReceipt> {
        val callerContext = coroutineContext
        var publishedRelease: CompletableDeferred<Unit>? = null
        val completed = try {
            mutex.withLock {
                callerContext.ensureActive()
                val old = mutableFrames.value

                if (old.closed) {
                    return@withLock MutationResult.Rejected(FinancialError.SessionClosed)
                }

                val commandIdentifier = command.meta.commandIdentifier

                if (commandIdentifier.isBlank() || commandIdentifier.length > 128) {
                    return@withLock MutationResult.Rejected(
                        FinancialError.Validation(
                            "meta.commandIdentifier",
                            "A valid command ID is required"
                        )
                    )
                }

                val retained = retainedReceipts[commandIdentifier]

                if (retained != null) {
                    return@withLock if (retained.command == command) {
                        val receipt = retained.receipt
                        MutationResult.Committed(receipt, receipt.storeRevision, receipt.changed)
                    } else {
                        MutationResult.Rejected(FinancialError.CommandIdentifierReused(commandIdentifier))
                    }
                }

                when (val reduction = reducer.reduce(old.data, old.revision, command)) {
                    is FinancialReduction.Rejected -> MutationResult.Rejected(reduction.error)
                    is FinancialReduction.Accepted -> {
                        if (!reduction.changed) {
                            val receipt = CommandReceipt(reduction.value, old.revision, false)
                            val retainedState = retain(commandIdentifier, command, receipt)
                            retainedReceipts = retainedState.first
                            receiptOrder = retainedState.second
                            MutationResult.Committed(receipt, old.revision, false)
                        } else {
                            val nextRevision = try {
                                incrementRevision(old.revision)
                            } catch (_: ArithmeticOverflowException) {
                                return@withLock MutationResult.Rejected(
                                    FinancialError.Validation(
                                        "revision",
                                        "The session revision limit has been reached"
                                    )
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
                            val retainedState = retain(commandIdentifier, command, receipt)
                            retainedReceipts = retainedState.first
                            receiptOrder = retainedState.second
                            publishedRelease = released
                            mutableFrames.value = nextFrame
                            MutationResult.Committed(receipt, nextRevision, true)
                        }
                    }
                }
            }
        } finally {
            publishedRelease?.complete(Unit)
        }

        return completed
    }

    suspend fun close() {
        var publishedRelease: CompletableDeferred<Unit>? = null
        var sessionClosed = false

        try {
            mutex.withLock {
                val old = mutableFrames.value

                if (old.closed) {
                    sessionClosed = true
                    return@withLock
                }

                val nextRevision = if (old.revision == Long.MAX_VALUE) old.revision else old.revision + 1
                val fence = CompletableDeferred<Unit>()
                val empty = initialData

                publishedRelease = fence
                mutableFrames.value = InternalFrame(
                    sessionIdentity = old.sessionIdentity,
                    revision = nextRevision,
                    data = empty,
                    journal = persistentListOf(),
                    closed = true,
                    released = fence,
                )
                sessionClosed = true
                retainedReceipts = persistentMapOf()
                receiptOrder = persistentListOf()
            }
        } finally {
            publishedRelease?.complete(Unit)

            if (sessionClosed) {
                sessionLifetime.cancel(CancellationException("Financial session closed"))
            }
        }
    }

    private fun retain(
        commandIdentifier: String,
        command: FinancialCommand,
        receipt: CommandReceipt,
    ): Pair<kotlinx.collections.immutable.PersistentMap<String, RetainedCommandReceipt>, kotlinx.collections.immutable.PersistentList<String>> {
        var nextMap = retainedReceipts.putting(commandIdentifier, RetainedCommandReceipt(command, receipt))
        var nextOrder = receiptOrder.adding(commandIdentifier)

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
                val identifier = command.command.operation.identifier
                operations = operations.adding(EntityDelta(before.operations[identifier], after.operations[identifier]))
            }

            is FinancialCommand.UpdateOperation -> {
                val identifier = command.command.identifier
                operations = operations.adding(EntityDelta(before.operations[identifier], after.operations[identifier]))
            }

            is FinancialCommand.RemoveOperation -> {
                val identifier = command.command.identifier
                operations = operations.adding(EntityDelta(before.operations[identifier], after.operations[identifier]))
            }

            is FinancialCommand.SavePlanning -> {
                val identifier = command.command.table.identifier
                tables = tables.adding(EntityDelta(before.planningTables[identifier], after.planningTables[identifier]))
            }

            is FinancialCommand.DeletePlanning -> {
                val identifier = command.command.identifier
                tables = tables.adding(EntityDelta(before.planningTables[identifier], after.planningTables[identifier]))
            }

            is FinancialCommand.CreateCategory -> {
                val identifier = command.command.identifier
                categories = categories.adding(EntityDelta(before.categories[identifier], after.categories[identifier]))
            }

            is FinancialCommand.UpdateCategory -> {
                val identifier = command.command.identifier
                categories = categories.adding(EntityDelta(before.categories[identifier], after.categories[identifier]))
            }

            is FinancialCommand.ArchiveCategory -> {
                val identifier = command.command.identifier
                categories = categories.adding(EntityDelta(before.categories[identifier], after.categories[identifier]))
            }

            is FinancialCommand.DeleteCategory -> {
                val identifier = command.command.identifier
                categories = categories.adding(EntityDelta(before.categories[identifier], after.categories[identifier]))
            }
        }

        return FinancialCommit(revision, command.meta.commandIdentifier, before, after, operations, categories, tables)
    }
}

private fun <T> List<T>.takeLastPersistent(limit: Int) =
    takeLast(limit).toPersistentList()
