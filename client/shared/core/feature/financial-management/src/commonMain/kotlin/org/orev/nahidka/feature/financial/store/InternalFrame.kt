package org.orev.nahidka.feature.financial.store

import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CompletableDeferred

internal data class InternalFrame(
    val sessionIdentity: String,
    val revision: Long,
    val data: FinancialData,
    val journal: PersistentList<FinancialCommit>,
    val closed: Boolean,
    val released: CompletableDeferred<Unit>,
)
