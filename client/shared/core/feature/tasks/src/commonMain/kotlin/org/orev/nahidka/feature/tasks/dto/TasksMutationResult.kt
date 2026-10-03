package org.orev.nahidka.feature.tasks.dto

class TasksMutationResult(
    val revision: Long,
    affectedTasks: List<TaskRecord>
) {
    private val taskRecords = affectedTasks.toMutableList()

    val affectedTasks: List<TaskRecord>
        get() = taskRecords.toMutableList()
}
