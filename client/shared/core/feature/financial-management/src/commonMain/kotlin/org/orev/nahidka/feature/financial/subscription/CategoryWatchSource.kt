package org.orev.nahidka.feature.financial.subscription

import org.orev.nahidka.feature.financial.calculation.FinancialCategoryNameComparator
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.financial.dto.CategoryQuery
import org.orev.nahidka.feature.financial.dto.EntityChange
import org.orev.nahidka.feature.financial.dto.EntitySnapshot
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialChangeBatch
import org.orev.nahidka.feature.financial.store.FinancialCommit
import org.orev.nahidka.feature.financial.store.InternalFrame
import org.orev.nahidka.core.common.ErrorReporter

internal class CategoryWatchSource(
    override val frames: StateFlow<InternalFrame>,
    override val sessionLifetime: Job,
    override val errorReporter: ErrorReporter,
    private val query: CategoryQuery,
) : WatchSource<FinancialCategory> {

    override fun currentFrame(): InternalFrame = frames.value

    override fun snapshot(frame: InternalFrame): EntitySnapshot<FinancialCategory> = EntitySnapshot(
        sessionIdentity = frame.sessionIdentity,
        storeRevision = frame.revision,
        entities = frame.data.categories.values
            .filter { query.includeArchived || !it.archived }
            .sortedWith(FinancialCategoryNameComparator)
            .toPersistentList(),
    )

    override fun project(commit: FinancialCommit): FinancialChangeBatch<FinancialCategory>? {
        val changes = mutableListOf<EntityChange<FinancialCategory>>()
        commit.categoryDeltas.forEachIndexed { index, delta ->
            val before = delta.before?.takeIf {
                query.includeArchived || !it.archived
            }

            val after = delta.after?.takeIf {
                query.includeArchived || !it.archived
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

                before != null && after != null && before != after -> changes += EntityChange.Update(
                    before,
                    after,
                    commit.revision,
                    commit.commandIdentifier,
                    index
                )
            }
        }

        if (changes.isEmpty()) {
            return null
        }

        return FinancialChangeBatch(commit.revision, commit.commandIdentifier, changes.toPersistentList())
    }
}
