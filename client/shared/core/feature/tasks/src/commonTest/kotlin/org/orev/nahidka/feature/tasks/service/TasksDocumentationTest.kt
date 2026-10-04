package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import kotlin.test.Test

class TasksDocumentationTest {

    @Test
    fun documentedCrudExample() = runTest {
        taskExample()
    }

    @Test
    fun documentedRatingsExample() = runTest {
        ratingExample()
    }
}

private suspend fun taskExample() {
    val context = TasksContext()
    val manager = TasksManager(context)
    check(context.currentSnapshot().revision == 0L)

    val created = manager.createTasks(listOf(
        TaskCreationRequest("read-docs", "Read docs"),
        TaskCreationRequest("ship", "Ship feature", description = "Review first", priority = 2),
    ))
    check(created.revision == 1L && created.affectedTasks.size == 2)

    val updated = manager.updateTasks(listOf(
        TaskUpdateRequest("read-docs", status = TaskStatus.DONE),
        TaskUpdateRequest("ship", descriptionPatch = NullablePatch.Clear),
    ))
    check(updated.changed && updated.revision == 2L)

    val unchanged = manager.updateTasks(listOf(TaskUpdateRequest("read-docs")))
    check(!unchanged.changed && unchanged.affectedTasks.isEmpty())
    check(unchanged.revision == updated.revision)

    val deleted = manager.deleteTasks(listOf("read-docs", "ship"))
    check(deleted.affectedTasks.size == 2)
    check(context.currentSnapshot().tasks.isEmpty())
}

private suspend fun ratingExample() {
    val context = TasksContext()
    context.createTasks(listOf(TaskCreationRequest("ship", "Ship feature", dueDate = LocalDate(2026, 10, 31))))
    context.updateTasks(listOf(
        TaskUpdateRequest("ship", status = TaskStatus.DONE, ratingIdentifierPatch = NullablePatch.Set("excellent")),
    ))

    context.updateTasks(listOf(TaskUpdateRequest("ship", status = TaskStatus.IN_PROGRESS)))
    check(context.currentSnapshot().tasks.single().ratingIdentifier == null)

    val replaced = context.replaceRatingLevels(listOf(
        TaskRatingLevel("bad", "👎"),
        TaskRatingLevel("excellent", "🔥"),
    ))
    check(replaced.changed && replaced.affectedTasks.isEmpty())
}


