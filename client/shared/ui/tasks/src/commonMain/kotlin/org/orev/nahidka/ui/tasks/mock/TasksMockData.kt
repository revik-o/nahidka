package org.orev.nahidka.ui.tasks.mock

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.feature.tasks.service.TasksManager

object TasksMockData {

    suspend fun seed(tasksManager: TasksManager) {
        tasksManager.createTasks(
            listOf(
                TaskCreationRequest(
                    identifier = "demo-weekly-plan",
                    title = "Plan the week",
                    description = "Choose three priorities and make room for rest, friends, and focused work.",
                    dueDate = LocalDate(2026, 10, 5),
                ),
                TaskCreationRequest(
                    identifier = "demo-reading",
                    title = "Start a new book",
                    description = "Pick something from the reading list and read the first chapter.",
                ),
                TaskCreationRequest(
                    identifier = "demo-weekend",
                    title = "Arrange a weekend walk",
                    description = "Find a new route and invite a friend. Bring a warm drink if the weather is cool.",
                    dueDate = LocalDate(2026, 10, 10),
                ),
                TaskCreationRequest(
                    identifier = "demo-budget",
                    title = "Review the monthly budget",
                    description = "Check recent expenses and adjust next month's plan.",
                    status = TaskStatus.IN_PROGRESS,
                    dueDate = LocalDate(2026, 10, 7),
                ),
                TaskCreationRequest(
                    identifier = "demo-desk",
                    title = "Organize the workspace",
                    description = "Clear the desk, sort notes, and set up a comfortable place to focus.",
                    status = TaskStatus.DONE,
                    dueDate = LocalDate(2026, 10, 4),
                    ratingIdentifier = "excellent",
                ),
                TaskCreationRequest(
                    identifier = "demo-catch-up",
                    title = "Catch up with a friend",
                    description = "Make time for a conversation away from the daily rush.",
                    status = TaskStatus.DONE,
                ),
            ),
        )
    }
}
