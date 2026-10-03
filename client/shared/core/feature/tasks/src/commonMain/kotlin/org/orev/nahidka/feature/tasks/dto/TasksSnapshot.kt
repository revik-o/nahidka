package org.orev.nahidka.feature.tasks.dto

class TasksSnapshot(
    override val revision: Long,
    tasks: List<TaskRecord>
) : TasksNotification {
    private val taskRecords = tasks.toMutableList()

    val tasks: List<TaskRecord>
        get() = taskRecords.toMutableList()
}
