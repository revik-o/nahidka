package org.orev.nahidka.ui.tasks

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem
import org.orev.nahidka.ui.tasks.model.TasksContent
import kotlin.test.*

class TasksViewModelTest : TasksTest() {

    @Test
    fun taskCreationAddsTaskToToDoColumn() = runTest {
        createTask("Write plan")

        val createdTaskItem = awaitContent { tasksContent -> tasksContent.taskItems.isNotEmpty() }
            .taskItemsWithStatus(TaskStatus.TO_DO)
            .single()

        assertEquals("Write plan", createdTaskItem.task.title)
        assertNull(tasksViewModel.taskEditor.dialogState.value)
    }

    @Test
    fun taskDraftWithBlankTitleIsNotSubmittable() {
        tasksViewModel.openTaskCreation()
        tasksViewModel.taskEditor.edit { taskDraft -> taskDraft.copy(title = "  ") }

        assertFalse(checkNotNull(tasksViewModel.taskEditor.dialogState.value).submittable)
    }

    @Test
    fun taskEditingSavesDueDateAndKeepsRating() = runTest {
        val dueDate = LocalDate(2026, 10, 31)
        val createdTaskItem = createDoneRatedTask("Ship feature")

        tasksViewModel.openTaskEditing(createdTaskItem)
        tasksViewModel.taskEditor.edit { taskDraft -> taskDraft.copy(dueDate = dueDate) }
        tasksViewModel.taskEditor.submit()

        val editedTaskItem = awaitSingleTaskItem { taskItem -> taskItem.task.dueDate == dueDate }
        assertEquals(TaskStatus.DONE, editedTaskItem.task.status)
        assertEquals(createdTaskItem.ratingLevel, editedTaskItem.ratingLevel)
    }

    @Test
    fun movingTaskOutOfDoneClearsRating() = runTest {
        val createdTaskItem = createDoneRatedTask("Ship feature")

        tasksViewModel.moveTask(createdTaskItem, TaskStatus.IN_PROGRESS)

        val movedTaskItem = awaitSingleTaskItem { taskItem -> taskItem.task.status == TaskStatus.IN_PROGRESS }
        assertNull(movedTaskItem.ratingLevel)
    }

    @Test
    fun removingRatingLevelClearsItFromRatedTask() = runTest {
        val createdTaskItem = createDoneRatedTask("Ship feature")
        val usedRatingLevelIndex = awaitContent { true }.ratingLevels.indexOf(createdTaskItem.ratingLevel)

        tasksViewModel.openRatingLevelsEditing()
        tasksViewModel.ratingLevelsEditor.edit { ratingLevelsDraft -> ratingLevelsDraft.removing(usedRatingLevelIndex) }
        tasksViewModel.ratingLevelsEditor.submit()

        val tasksContent = awaitContent { content -> content.taskItems.single().ratingLevel == null }
        assertFalse(createdTaskItem.ratingLevel in tasksContent.ratingLevels)
    }

    @Test
    fun addedReactionMustNotBeBlankBeforeSaving() = runTest {
        tasksViewModel.openRatingLevelsEditing()
        tasksViewModel.addRatingLevel()
        assertFalse(checkNotNull(tasksViewModel.ratingLevelsEditor.dialogState.value).submittable)

        tasksViewModel.ratingLevelsEditor.edit { ratingLevelsDraft ->
            ratingLevelsDraft.replacingReaction(ratingLevelsDraft.ratingLevels.lastIndex, "🔥")
        }
        tasksViewModel.ratingLevelsEditor.submit()

        awaitContent { tasksContent -> tasksContent.ratingLevels.last().reaction == "🔥" }
    }

    @Test
    fun taskDeletionRemovesTask() = runTest {
        createTask("Write plan")
        val createdTaskItem = awaitSingleTaskItem { true }

        tasksViewModel.taskInteractions.onTaskDelete(createdTaskItem)
        tasksViewModel.taskDeletion.submit()

        awaitContent { tasksContent -> tasksContent.taskItems.isEmpty() }
    }

    private suspend fun createDoneRatedTask(title: String): TaskItem {
        createTask(title, TaskStatus.DONE)
        val createdTaskItem = awaitSingleTaskItem { true }
        val bestRatingLevel = awaitContent { true }.ratingLevels.last()

        tasksViewModel.rateTask(createdTaskItem, bestRatingLevel)

        return awaitSingleTaskItem { taskItem -> taskItem.ratingLevel == bestRatingLevel }
    }

    private suspend fun awaitSingleTaskItem(taskItemExpectation: (TaskItem) -> Boolean): TaskItem =
        awaitContent { tasksContent ->
            tasksContent.taskItems
                .singleOrNull()
                ?.let(taskItemExpectation) == true
        }
            .taskItems
            .single()

    private suspend fun awaitContent(contentExpectation: (TasksContent) -> Boolean): TasksContent =
        tasksViewModel.tasksContent.first(contentExpectation)
}
