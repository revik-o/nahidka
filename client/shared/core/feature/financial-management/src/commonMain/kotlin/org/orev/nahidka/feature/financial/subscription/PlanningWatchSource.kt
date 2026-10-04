package org.orev.nahidka.feature.financial.subscription

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.core.common.ErrorReporter
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.calculation.calculatePlanningTableView
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.FinancialCommit
import org.orev.nahidka.feature.financial.store.FinancialData
import org.orev.nahidka.feature.financial.store.InternalFrame

internal class PlanningWatchSource(
    override val frames: StateFlow<InternalFrame>,
    override val sessionLifetime: Job,
    override val errorReporter: ErrorReporter,
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
            .sortedWith(compareBy<FinancialPlanningTable> { it.month }.thenBy { it.assetIdentifier }
                .thenBy { it.identifier })
            .map { project(it, frame.data) }
            .toPersistentList(),
    )

    override fun project(commit: FinancialCommit): FinancialChangeBatch<PlanningTableView>? {
        val affected = linkedMapOf<String, Int>()

        commit.planningDeltas.forEachIndexed { index, delta ->
            delta.before?.let {
                affected[it.identifier] = index
            }
            delta.after?.let {
                affected[it.identifier] = index
            }
        }

        commit.operationDeltas.forEachIndexed { index, delta ->
            listOfNotNull(delta.before, delta.after).forEach { operation ->
                val month = financialMonthFor(operation, config.reportingTimeZone)

                listOf(commit.before, commit.after).forEach { data ->
                    data.planningTables.values.firstOrNull {
                        it.month == month && it.assetIdentifier == operation.amount.assetIdentifier
                    }?.let { retainFirstIndex(affected, it.identifier, index) }
                }
            }
        }

        commit.categoryDeltas.forEachIndexed { index, delta ->
            val categoryIdentifier = delta.before?.identifier ?: delta.after?.identifier ?: return@forEachIndexed

            listOf(commit.before, commit.after).forEach { data ->
                data.planningTables.values
                    .filter { table -> table.rows.any { it.categoryIdentifier == categoryIdentifier } }
                    .forEach { retainFirstIndex(affected, it.identifier, index) }
            }
        }

        val changes = mutableListOf<EntityChange<PlanningTableView>>()

        for ((tableIdentifier, index) in affected.entries.sortedBy { it.key }.map { it.key to it.value }) {
            val oldDocument = commit.before.planningTables[tableIdentifier]?.takeIf(::matches)
            val newDocument = commit.after.planningTables[tableIdentifier]?.takeIf(::matches)
            val before = oldDocument?.let {
                project(it, commit.before)
            }
            val after = newDocument?.let {
                project(it, commit.after)
            }

            when {
                before == null && after != null -> changes += EntityChange.Insert(
                    after,
                    commit.revision,
                    commit.commandIdentifier,
                    index
                )

                before != null && after == null -> changes += EntityChange.Delete(
                    before,
                    commit.revision,
                    commit.commandIdentifier,
                    index
                )

                before != null && after != null && before != after ->
                    changes += EntityChange.Update(before, after, commit.revision, commit.commandIdentifier, index)
            }
        }

        if (changes.isEmpty()) {
            return null
        }

        return FinancialChangeBatch(commit.revision, commit.commandIdentifier, changes.toPersistentList())
    }

    private fun matches(table: FinancialPlanningTable): Boolean =
        (query.month == null || table.month == query.month) &&
                (query.assetIdentifier == null || table.assetIdentifier == query.assetIdentifier)

    private fun retainFirstIndex(indices: MutableMap<String, Int>, identifier: String, index: Int) {
        if (identifier !in indices) indices[identifier] = index
    }

    private fun project(table: FinancialPlanningTable, data: FinancialData): PlanningTableView {
        val period = calendar.reportingPeriod(table.month, config.reportingTimeZone)
        return calculatePlanningTableView(table, data.operations.values, data.categories.values, period)
    }
}
