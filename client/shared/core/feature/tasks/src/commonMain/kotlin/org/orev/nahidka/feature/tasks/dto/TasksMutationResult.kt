package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

data class TasksMutationResult(
    val revision: Long,
    val affectedTasks: ImmutableList<TaskRecord>,
    val changed: Boolean = affectedTasks.isNotEmpty()
) {

    constructor(
        revision: Long,
        affectedTasks: List<TaskRecord>,
        changed: Boolean = affectedTasks.isNotEmpty()
    ) : this(revision, affectedTasks.toPersistentList(), changed)
}
