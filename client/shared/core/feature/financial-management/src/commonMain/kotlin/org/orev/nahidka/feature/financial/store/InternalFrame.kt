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

internal suspend fun InternalFrame.awaitRelease() {
    val isReleased = kotlinx.coroutines.withTimeoutOrNull(5_000L) {
        released.await()
        true
    } ?: false

    check(isReleased) {
        "Financial frame $revision was not released within five seconds"
    }
}
