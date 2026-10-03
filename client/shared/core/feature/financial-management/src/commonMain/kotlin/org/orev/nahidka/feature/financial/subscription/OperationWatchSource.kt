package org.orev.nahidka.feature.financial.subscription

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.financial.dto.EntityChange
import org.orev.nahidka.feature.financial.dto.EntitySnapshot
import org.orev.nahidka.feature.financial.dto.FinancialChangeBatch
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.store.FinancialCommit
import org.orev.nahidka.feature.financial.store.InternalFrame
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter

internal class OperationWatchSource(
    override val frames: StateFlow<InternalFrame>,
    override val sessionLifetime: Job,
    override val errorReporter: FinancialErrorReporter,
    private val query: OperationQuery,
) : WatchSource<FinancialOperation> {
    override fun currentFrame(): InternalFrame = frames.value

    override fun snapshot(frame: InternalFrame): EntitySnapshot<FinancialOperation> {
        val entities = frame.data.operations.values
            .filter(::matches)
            .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.id })
            .toPersistentList()
        return EntitySnapshot(frame.sessionIdentity, frame.revision, entities)
    }

    override fun project(commit: FinancialCommit): FinancialChangeBatch<FinancialOperation>? {
        val changes = mutableListOf<EntityChange<FinancialOperation>>()
        commit.operationDeltas.forEachIndexed { index, delta ->
            val before = delta.before?.takeIf(::matches)
            val after = delta.after?.takeIf(::matches)
            when {
                before == null && after != null -> changes += EntityChange.Insert(after, commit.revision, commit.commandId, index)
                before != null && after == null -> changes += EntityChange.Delete(before, commit.revision, commit.commandId, index)
                before != null && after != null && before != after -> changes += EntityChange.Update(before, after, commit.revision, commit.commandId, index)
            }
        }
        if (changes.isEmpty()) return null
        return FinancialChangeBatch(commit.revision, commit.commandId, changes.toPersistentList())
    }

    private fun matches(operation: FinancialOperation): Boolean =
        (query.period == null || operation.occurredAt >= query.period.startInclusive && operation.occurredAt < query.period.endExclusive) &&
            (query.assetId == null || operation.amount.assetId == query.assetId) &&
            (query.categoryId == null || operation.categoryId == query.categoryId) &&
            (query.kinds.isEmpty() || operation.kind in query.kinds)
}
