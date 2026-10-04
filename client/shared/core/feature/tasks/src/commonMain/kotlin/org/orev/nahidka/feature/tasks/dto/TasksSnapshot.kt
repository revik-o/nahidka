package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

data class TasksSnapshot(
    val revision: Long,
    private val recordsByIdentifier: PersistentMap<String, TaskRecord>
) {

    val tasks: ImmutableList<TaskRecord> by lazy {
        recordsByIdentifier.values.toPersistentList()
    }

    constructor(revision: Long, tasks: List<TaskRecord>) : this(
        revision, tasks.associateBy { it.identifier }.toPersistentMap()
    ) {
        require(tasks.size == recordsByIdentifier.size) { "Snapshot identifiers must be unique" }
    }
}
