package org.orev.nahidka.ui.tasks.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot

data class TasksContent(
    val taskItems: ImmutableList<TaskItem>,
    val ratingLevels: ImmutableList<TaskRatingLevel>,
) {

    fun taskItemsWithStatus(status: TaskStatus): List<TaskItem> =
        taskItems.filter { taskItem -> taskItem.task.status == status }

    companion object {

        fun of(tasksSnapshot: TasksSnapshot): TasksContent {
            val ratingLevelsByIdentifier = tasksSnapshot.ratingLevels.associateBy(TaskRatingLevel::identifier)

            return TasksContent(
                taskItems = tasksSnapshot.tasks
                    .map { task -> TaskItem(task, task.ratingIdentifier?.let(ratingLevelsByIdentifier::get)) }
                    .toPersistentList(),
                ratingLevels = tasksSnapshot.ratingLevels,
            )
        }
    }
}
