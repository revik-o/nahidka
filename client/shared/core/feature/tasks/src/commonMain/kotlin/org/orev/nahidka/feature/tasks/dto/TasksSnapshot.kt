package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

data class TasksSnapshot(
    val revision: Long,
    private val recordsByIdentifier: PersistentMap<String, TaskRecord>,
    val ratingLevels: ImmutableList<TaskRatingLevel>
) {

    val tasks: ImmutableList<TaskRecord> by lazy {
        recordsByIdentifier.values.toPersistentList()
    }

    constructor(
        revision: Long,
        tasks: List<TaskRecord>,
        ratingLevels: List<TaskRatingLevel> = DEFAULT_TASK_RATING_LEVELS
    ) : this(
        revision,
        tasks
            .associateBy { task -> task.identifier }
            .toPersistentMap(),
        ratingLevels.toPersistentList()
    ) {
        require(tasks.size == recordsByIdentifier.size) { "Snapshot identifiers must be unique" }
    }
}
