package org.orev.nahidka.feature.tasks.dto

import kotlinx.datetime.LocalDate

data class TaskRecord(
    val identifier: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val priority: Int = 0,
    val dueDate: LocalDate? = null,
    val ratingIdentifier: String? = null
)
