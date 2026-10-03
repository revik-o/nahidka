package org.orev.nahidka.feature.tasks.dto

class TasksUpdated(
    override val revision: Long,
    taskChanges: List<TaskChange>
) : TasksNotification {
    private val changes = taskChanges.toMutableList()

    val taskChanges: List<TaskChange>
        get() = changes.toMutableList()
}
