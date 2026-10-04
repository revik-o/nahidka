package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

data class TasksMutationResult(
    val revision: Long,
    val affectedTasks: ImmutableList<TaskRecord>
) {

    constructor(revision: Long, affectedTasks: List<TaskRecord>) : this(revision, affectedTasks.toPersistentList())

    val changed: Boolean get() = affectedTasks.isNotEmpty()
}
