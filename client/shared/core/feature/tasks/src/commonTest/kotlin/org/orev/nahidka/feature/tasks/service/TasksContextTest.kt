package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import kotlin.test.*

class TasksContextTest {

    private val excellentRatingLevel = TaskRatingLevel("excellent", "🤩")

    private val tasksContext = TasksContext(
        initialTasks = listOf(
            TaskRecord(
                identifier = "ship",
                title = "Ship feature",
                status = TaskStatus.DONE,
                ratingIdentifier = excellentRatingLevel.identifier
            )
        ),
        initialRatingLevels = listOf(TaskRatingLevel("bad", "😞"), excellentRatingLevel)
    )

    @Test
    fun leavingDoneStatusClearsRating() = runTest {
        tasksContext.updateTasks(listOf(TaskUpdateRequest("ship", status = TaskStatus.IN_PROGRESS)))

        assertNull(tasksContext.currentSnapshot().tasks.single().ratingIdentifier)
    }

    @Test
    fun ratingTaskThatIsNotDoneIsRejected() = runTest {
        tasksContext.createTasks(listOf(TaskCreationRequest("read-docs", "Read docs")))

        assertFailsWith<IllegalArgumentException> {
            tasksContext.updateTasks(
                listOf(
                    TaskUpdateRequest(
                        identifier = "read-docs",
                        ratingIdentifierPatch = NullablePatch.Set(excellentRatingLevel.identifier)
                    )
                )
            )
        }
    }

    @Test
    fun removingRatingLevelClearsItFromRatedTasks() = runTest {
        val mutationResult = tasksContext.replaceRatingLevels(listOf(TaskRatingLevel("bad", "😞")))

        assertTrue(mutationResult.changed)
        assertEquals(listOf("ship"), mutationResult.affectedTasks.map(TaskRecord::identifier))
        assertNull(tasksContext.currentSnapshot().tasks.single().ratingIdentifier)
    }

    @Test
    fun changingReactionAdvancesRevisionWithoutAffectingTasks() = runTest {
        val mutationResult = tasksContext.replaceRatingLevels(
            listOf(TaskRatingLevel("bad", "😞"), excellentRatingLevel.copy(reaction = "🔥"))
        )

        assertTrue(mutationResult.changed)
        assertTrue(mutationResult.affectedTasks.isEmpty())
        assertEquals(1L, tasksContext.currentSnapshot().revision)
        assertEquals("🔥", tasksContext.currentSnapshot().ratingLevels.last().reaction)
    }

    @Test
    fun dueDatePatchSetsAndClearsDueDate() = runTest {
        val dueDate = LocalDate(2026, 10, 31)

        tasksContext.updateTasks(listOf(TaskUpdateRequest("ship", dueDatePatch = NullablePatch.Set(dueDate))))
        assertEquals(dueDate, tasksContext.currentSnapshot().tasks.single().dueDate)

        tasksContext.updateTasks(listOf(TaskUpdateRequest("ship", dueDatePatch = NullablePatch.Clear)))
        assertNull(tasksContext.currentSnapshot().tasks.single().dueDate)
    }

    @Test
    fun invalidUpdateBatchLeavesTasksAndRevisionUnchanged() = runTest {
        val previousSnapshot = tasksContext.currentSnapshot()

        assertFailsWith<IllegalArgumentException> {
            tasksContext.updateTasks(
                listOf(
                    TaskUpdateRequest("ship", title = "Updated title"),
                    TaskUpdateRequest("missing", title = "Another title"),
                ),
            )
        }

        assertEquals(previousSnapshot, tasksContext.currentSnapshot())
    }

    @Test
    fun taskOrderSurvivesUpdatesAndDeletion() = runTest {
        val context = TasksContext()
        context.createTasks(
            listOf(
                TaskCreationRequest("z", "First"),
                TaskCreationRequest("a", "Second"),
                TaskCreationRequest("m", "Third"),
            ),
        )
        val originalSnapshot = context.currentSnapshot()
        context.updateTasks(listOf(TaskUpdateRequest("z", status = TaskStatus.DONE)))
        assertEquals(listOf("z", "a", "m"), context.currentSnapshot().tasks.map(TaskRecord::identifier))
        context.deleteTasks(listOf("a"))
        assertEquals(listOf("z", "m"), context.currentSnapshot().tasks.map(TaskRecord::identifier))
        assertEquals(TaskStatus.TO_DO, originalSnapshot.tasks.first().status)
        assertEquals(3, originalSnapshot.tasks.size)
    }

    @Test
    fun invalidRatingLevelsLeaveTasksAndReactionsUnchanged() = runTest {
        val previousSnapshot = tasksContext.currentSnapshot()
        assertFailsWith<IllegalArgumentException> {
            tasksContext.replaceRatingLevels(
                listOf(TaskRatingLevel("duplicate", "👍"), TaskRatingLevel("duplicate", "👎")),
            )
        }
        assertEquals(previousSnapshot, tasksContext.currentSnapshot())
        assertFailsWith<IllegalArgumentException> {
            tasksContext.replaceRatingLevels(listOf(TaskRatingLevel("blank", " ")))
        }
        assertEquals(previousSnapshot, tasksContext.currentSnapshot())
    }
}
