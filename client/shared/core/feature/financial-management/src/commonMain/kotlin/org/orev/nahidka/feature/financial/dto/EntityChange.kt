package org.orev.nahidka.feature.financial.dto

sealed interface EntityChange<out T> {
    val storeRevision: Long
    val commandId: String
    val index: Int

    data class Insert<T>(
        val after: T,
        override val storeRevision: Long,
        override val commandId: String,
        override val index: Int,
    ) : EntityChange<T>

    data class Update<T>(
        val before: T,
        val after: T,
        override val storeRevision: Long,
        override val commandId: String,
        override val index: Int,
    ) : EntityChange<T>

    data class Delete<T>(
        val before: T,
        override val storeRevision: Long,
        override val commandId: String,
        override val index: Int,
    ) : EntityChange<T>
}
