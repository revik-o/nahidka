package org.orev.nahidka.feature.financial.subscription

import org.orev.nahidka.feature.financial.dto.EntitySnapshot
import org.orev.nahidka.feature.financial.dto.FinancialChangeBatch

sealed interface WatchMessage<out T> {
    data class Snapshot<T>(val snapshot: EntitySnapshot<T>) : WatchMessage<T>
    data class Resync<T>(val snapshot: EntitySnapshot<T>) : WatchMessage<T>
    data class Batch<T>(val batch: FinancialChangeBatch<T>) : WatchMessage<T>
}
