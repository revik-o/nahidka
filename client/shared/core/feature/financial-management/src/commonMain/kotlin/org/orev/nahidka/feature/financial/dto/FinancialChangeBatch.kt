package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList

data class FinancialChangeBatch<T>(
    val storeRevision: Long,
    val commandId: String,
    val changes: PersistentList<EntityChange<T>>,
)
