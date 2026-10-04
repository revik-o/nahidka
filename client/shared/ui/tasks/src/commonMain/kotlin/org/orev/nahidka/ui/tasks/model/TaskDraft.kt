package org.orev.nahidka.ui.tasks.model

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.nullablePatch
import org.orev.nahidka.feature.tasks.dto.*

data class TaskDraft(
    val editedTask: TaskRecord?,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val dueDate: LocalDate?,
    val ratingLevel: TaskRatingLevel?,
) {

    val submittable: Boolean
        get() = title.isNotBlank()

    fun withStatus(status: TaskStatus): TaskDraft =
        copy(status = status, ratingLevel = ratingLevel.takeIf { status.acceptsRating })

    fun toCreationRequest(identifier: String): TaskCreationRequest = TaskCreationRequest(
        identifier = identifier,
        title = title.trim(),
        description = description.trim(),
        status = status,
        dueDate = dueDate,
        ratingIdentifier = ratingLevel?.identifier,
    )

    fun toUpdateRequest(editedTask: TaskRecord): TaskUpdateRequest = TaskUpdateRequest(
        identifier = editedTask.identifier,
        title = title.trim(),
        descriptionPatch = nullablePatch(editedTask.description, description.trim()),
        status = status,
        dueDatePatch = nullablePatch(editedTask.dueDate, dueDate),
        ratingIdentifierPatch = nullablePatch(editedTask.ratingIdentifier, ratingLevel?.identifier),
    )

    companion object {

        fun creation(): TaskDraft = TaskDraft(
            editedTask = null,
            title = "",
            description = "",
            status = TaskStatus.TO_DO,
            dueDate = null,
            ratingLevel = null,
        )

        fun editing(taskItem: TaskItem): TaskDraft = TaskDraft(
            editedTask = taskItem.task,
            title = taskItem.task.title,
            description = taskItem.task.description,
            status = taskItem.task.status,
            dueDate = taskItem.task.dueDate,
            ratingLevel = taskItem.ratingLevel,
        )
    }
}
