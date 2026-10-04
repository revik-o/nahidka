package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TASK_TITLE = "Write plan"

@OptIn(ExperimentalTestApi::class)
class TasksScreenTest : TasksTest() {

    @Test
    fun draggingCardWithMouseMovesTaskToDoneColumn() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)
        val dragDistance = dragDistanceTo("Done (0)")

        onNodeWithText(TASK_TITLE)
            .performMouseInput {
                moveTo(center)
                press()
                moveBy(dragDistance)
                release()
            }

        waitUntil { taskItem()?.task?.status == TaskStatus.DONE }
    }

    @Test
    fun draggingCardAfterLongPressMovesTaskToInProgressColumn() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)
        val dragDistance = dragDistanceTo("In Progress (0)")

        onNodeWithText(TASK_TITLE)
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(dragDistance)
                up()
            }

        waitUntil { taskItem()?.task?.status == TaskStatus.IN_PROGRESS }
    }

    @Test
    fun clickingCardOpensTaskEditor() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)

        onNodeWithText(TASK_TITLE)
            .performClick()

        onNodeWithText("Edit task")
            .assertIsDisplayed()
    }

    @Test
    fun compactCardMenuMovesTaskToDone() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 400.dp)

        onAllNodesWithContentDescription("More actions")
            .onLast()
            .performClick()
        onNodeWithText("Move to Done")
            .performClick()
        onNodeWithText("Done (1)")
            .performClick()

        onNodeWithText(TASK_TITLE)
            .assertIsDisplayed()
        waitUntil { taskItem()?.task?.status == TaskStatus.DONE }
    }

    @Test
    fun listViewRatesDoneTaskWithReaction() = runComposeUiTest {
        createTask(TASK_TITLE, TaskStatus.DONE)
        showTasksScreen(width = 1200.dp)

        onNodeWithText("List")
            .performClick()
        onNodeWithText("Rate")
            .performClick()
        onNodeWithText("🤩")
            .performClick()

        onNodeWithText("🤩")
            .assertIsDisplayed()
        waitUntil { taskItem()?.ratingLevel?.reaction == "🤩" }
    }

    @Test
    fun choosingSelectedReactionAgainRemovesRating() = runComposeUiTest {
        createTask(TASK_TITLE, TaskStatus.DONE)
        showTasksScreen(width = 1200.dp)
        onNodeWithText("Rate")
            .performClick()
        onNodeWithText("😞")
            .performClick()

        onNodeWithText("😞")
            .performClick()
        onAllNodesWithText("😞")
            .onLast()
            .performClick()

        onNodeWithText("Rate")
            .assertIsDisplayed()
        waitUntil { taskItem()?.ratingLevel == null }
    }

    @Test
    fun creationDialogSavesTitleDescriptionAndDoneReaction() = runComposeUiTest {
        showTasksScreen(width = 1200.dp)
        onNodeWithText("Create new task")
            .performClick()
        onNodeWithText("Save")
            .assertIsNotEnabled()
        onNodeWithText("Title")
            .performTextInput(TASK_TITLE)
        onNodeWithText("Description")
            .performTextInput("Review the implementation")
        onNodeWithText("To Do", useUnmergedTree = true)
            .performClick()
        onNodeWithText("Done")
            .performClick()
        onNodeWithText("🤩")
            .performClick()
        onNodeWithText("Save")
            .performClick()
        onNodeWithText(TASK_TITLE)
            .assertIsDisplayed()
        waitUntil { taskItem()?.ratingLevel?.reaction == "🤩" }
        assertEquals("Review the implementation", taskItem()?.task?.description)
    }

    @Test
    fun deletingTaskRequiresConfirmationAndCanBeCancelled() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 400.dp)
        onAllNodesWithContentDescription("More actions")
            .onLast()
            .performClick()
        onNodeWithText("Delete")
            .performClick()
        onNodeWithText("Delete task?")
            .assertIsDisplayed()
        onNodeWithText("Cancel")
            .performClick()
        onNodeWithText(TASK_TITLE)
            .assertIsDisplayed()
        onAllNodesWithContentDescription("More actions")
            .onLast()
            .performClick()
        onNodeWithText("Delete")
            .performClick()
        onNodeWithText("Delete")
            .performClick()
        waitUntil { taskItem() == null }
        onNodeWithText("No tasks")
            .assertIsDisplayed()
    }

    @Test
    fun reactionCustomizationEditsAndReordersRatedTaskReaction() = runComposeUiTest {
        createTask(TASK_TITLE, TaskStatus.DONE)
        showTasksScreen(width = 1200.dp)
        onNodeWithText("Rate")
            .performClick()
        onNodeWithText("🤩")
            .performClick()
        onAllNodesWithContentDescription("More actions")
            .onFirst()
            .performClick()
        onNodeWithText("Rating reactions")
            .performClick()
        onNodeWithText("Reaction 4")
            .performTextReplacement("🔥 Great")
        onAllNodesWithContentDescription("More actions")
            .onLast()
            .performClick()
        onNodeWithText("Move up")
            .performClick()
        onNodeWithText("Save")
            .performClick()
        onNodeWithText("🔥 Great")
            .assertIsDisplayed()
        waitUntil { taskItem()?.ratingLevel?.reaction == "🔥 Great" }
        assertEquals("excellent", tasksViewModel.tasksContent.value.ratingLevels[2].identifier)
    }

    @Test
    fun datePickerOpensAndOptionalDueDateCanBeCleared() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 400.dp)
        onNodeWithText(TASK_TITLE)
            .performClick()
        tasksViewModel.taskEditor.edit { taskDraft -> taskDraft.copy(dueDate = LocalDate(2026, 10, 5)) }
        onNodeWithText("Due date")
            .performTouchInput { click() }
        onNodeWithText("Select")
            .assertIsDisplayed()
        onNodeWithText("Select")
            .performClick()
        onNodeWithContentDescription("Clear")
            .performClick()
        onNodeWithText("Save")
            .performClick()
        waitUntil { tasksViewModel.taskEditor.dialogState.value == null }
        assertEquals(null, taskItem()?.task?.dueDate)
    }

    @Test
    fun failedEditKeepsDialogOpenAndShowsRejection() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)
        onNodeWithText(TASK_TITLE)
            .performClick()
        val taskIdentifier = checkNotNull(taskItem()).task.identifier
        tasksScreenGraph.tasksManager.deleteTasks(listOf(taskIdentifier))
        onNodeWithText("Save")
            .performClick()
        onNodeWithText("This change could not be saved")
            .assertIsDisplayed()
        onNodeWithText("Edit task")
            .assertIsDisplayed()
    }

    private fun ComposeUiTest.showTasksScreen(width: Dp) {
        setContent {
            TasksScreen(tasksViewModel, Modifier.size(width, 800.dp))
        }
    }

    private fun ComposeUiTest.dragDistanceTo(targetText: String): Offset =
        centerOf(targetText) - centerOf(TASK_TITLE)

    private fun ComposeUiTest.centerOf(text: String): Offset = onNodeWithText(text)
        .fetchSemanticsNode()
        .boundsInRoot
        .center

    private fun taskItem(): TaskItem? = tasksViewModel.tasksContent.value.taskItems
        .firstOrNull { taskItem -> taskItem.task.title == TASK_TITLE }
}
