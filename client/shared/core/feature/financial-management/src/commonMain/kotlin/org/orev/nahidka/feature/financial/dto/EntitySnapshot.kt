package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentList

data class EntitySnapshot<T>(
    val sessionIdentity: String,
    val storeRevision: Long,
    val entities: PersistentList<T>,
)
