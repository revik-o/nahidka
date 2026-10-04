package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

data class OperationQuery(
    val period: ReportingPeriod? = null,
    val assetIdentifier: String? = null,
    val categoryIdentifier: String? = null,
    val kinds: PersistentSet<OperationKind> = persistentSetOf(),
)
