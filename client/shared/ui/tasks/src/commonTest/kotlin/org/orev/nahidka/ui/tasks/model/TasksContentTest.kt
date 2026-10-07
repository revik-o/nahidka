package org.orev.nahidka.ui.tasks.model

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.tasks.dto.TaskRecord
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksContentTest {

    @Test
    fun upcomingTasksExcludeDoneAndPutUndatedLast() {
        val tasksContent = TasksContent.of(
            TasksSnapshot(
                revision = 1,
                tasks = listOf(
                    TaskRecord("undated", "Undated"),
                    TaskRecord("later", "Later", dueDate = LocalDate(2026, 10, 20)),
                    TaskRecord("done", "Done", status = TaskStatus.DONE, dueDate = LocalDate(2026, 10, 1)),
                    TaskRecord("sooner", "Sooner", status = TaskStatus.IN_PROGRESS, dueDate = LocalDate(2026, 10, 8)),
                ),
            ),
        )

        assertEquals(
            listOf("sooner", "later", "undated"),
            tasksContent.upcomingTaskItems.map { taskItem -> taskItem.task.identifier },
        )
    }
}
