package org.orev.nahidka.feature.financial.subscription

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.calculation.calculatePlanningTableView
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.dto.EntityChange
import org.orev.nahidka.feature.financial.dto.EntitySnapshot
import org.orev.nahidka.feature.financial.dto.FinancialChangeBatch
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.PlanningQuery
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.store.FinancialCommit
import org.orev.nahidka.feature.financial.store.FinancialData
import org.orev.nahidka.feature.financial.store.InternalFrame
import org.orev.nahidka.feature.financial.support.FinancialErrorReporter

internal class PlanningWatchSource(
    override val frames: StateFlow<InternalFrame>,
    override val sessionLifetime: Job,
    override val errorReporter: FinancialErrorReporter,
    private val query: PlanningQuery,
    private val config: FinancialSessionConfig,
    private val calendar: FinancialCalendar,
) : WatchSource<PlanningTableView> {
    override fun currentFrame(): InternalFrame = frames.value

    override fun snapshot(frame: InternalFrame): EntitySnapshot<PlanningTableView> = EntitySnapshot(
        sessionIdentity = frame.sessionIdentity,
        storeRevision = frame.revision,
        entities = frame.data.planningTables.values
            .filter(::matches)
            .sortedWith(compareBy<FinancialPlanningTable> { it.input.month }.thenBy { it.input.assetId }.thenBy { it.input.id })
            .map { project(it, frame.data, frame.revision) }
            .toPersistentList(),
    )

    override fun project(commit: FinancialCommit): FinancialChangeBatch<PlanningTableView>? {
        val affected = linkedMapOf<String, Int>()
        commit.planningDeltas.forEachIndexed { index, delta ->
            delta.before?.let { affected[it.input.id] = index }
            delta.after?.let { affected[it.input.id] = index }
        }
        commit.operationDeltas.forEachIndexed { index, delta ->
            listOfNotNull(delta.before, delta.after).forEach { operation ->
                val month = financialMonthFor(operation, config.reportingTimeZone)
                listOf(commit.before, commit.after).forEach { data ->
                    data.planningTables.values.firstOrNull {
                        it.input.month == month && it.input.assetId == operation.amount.assetId
                    }?.let { retainFirstIndex(affected, it.input.id, index) }
                }
            }
        }
        commit.categoryDeltas.forEachIndexed { index, delta ->
            val categoryId = delta.before?.id ?: delta.after?.id ?: return@forEachIndexed
            listOf(commit.before, commit.after).forEach { data ->
                data.planningTables.values
                    .filter { table -> table.input.rows.any { it.categoryId == categoryId } }
                    .forEach { retainFirstIndex(affected, it.input.id, index) }
            }
        }
        val changes = mutableListOf<EntityChange<PlanningTableView>>()
        for ((tableId, index) in affected.entries.sortedBy { it.key }.map { it.key to it.value }) {
            val oldDocument = commit.before.planningTables[tableId]?.takeIf(::matches)
            val newDocument = commit.after.planningTables[tableId]?.takeIf(::matches)
            val before = oldDocument?.let { project(it, commit.before, commit.revision - 1) }
            val after = newDocument?.let { project(it, commit.after, commit.revision) }
            when {
                before == null && after != null -> changes += EntityChange.Insert(after, commit.revision, commit.commandId, index)
                before != null && after == null -> changes += EntityChange.Delete(before, commit.revision, commit.commandId, index)
                before != null && after != null && before.copy(storeRevision = 0) != after.copy(storeRevision = 0) ->
                    changes += EntityChange.Update(before, after, commit.revision, commit.commandId, index)
            }
        }
        if (changes.isEmpty()) return null
        return FinancialChangeBatch(commit.revision, commit.commandId, changes.toPersistentList())
    }

    private fun matches(table: FinancialPlanningTable): Boolean =
        (query.month == null || table.input.month == query.month) &&
            (query.assetId == null || table.input.assetId == query.assetId)

    private fun retainFirstIndex(indices: MutableMap<String, Int>, id: String, index: Int) {
        if (id !in indices) indices[id] = index
    }

    private fun project(table: FinancialPlanningTable, data: FinancialData, revision: Long): PlanningTableView {
        val period = calendar.reportingPeriod(table.input.month, config.reportingTimeZone)
        return calculatePlanningTableView(table, data.operations.values, data.categories.values, period, revision)
    }
}
