# Tasks

In-memory task CRUD with atomic batches and immutable snapshots. Each context owns an independent session; callers supply identifiers and persistence.

```kotlin
// Consumer build.gradle.kts — Android / JVM 11 / iOS arm64 + simulator arm64 / JS / Wasm JS
kotlin.sourceSets.commonMain.dependencies {
    implementation(project(":shared:core:feature:tasks"))
}
// Exports: :shared:core:lib:api, :shared:core:common, kotlinx-coroutines-core.
```

**Create, update, delete** — self-contained usage:

```kotlin
import org.orev.nahidka.api.TaskStatus
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager

suspend fun taskExample() {
    val context = TasksContext() // Or TasksContext(initialTasks = listOf(TaskRecord(...))).
    val manager = TasksManager(context)
    check(context.currentSnapshot().revision == 0L)

    val created = manager.createTasks(listOf(
        TaskCreationRequest("read-docs", "Read docs"),
        TaskCreationRequest("ship", "Ship feature", description = "Review first", priority = 2),
    ))
    check(created.revision == 1L && created.affectedTasks.size == 2) // One batch = one revision.

    val updated = manager.updateTasks(listOf(
        TaskUpdateRequest("read-docs", status = TaskStatus.DONE),
        TaskUpdateRequest("ship", descriptionPatch = NullablePatch.Clear), // description = ""
    ))
    check(updated.changed && updated.revision == 2L)

    val unchanged = manager.updateTasks(listOf(TaskUpdateRequest("read-docs")))
    check(!unchanged.changed && unchanged.affectedTasks.isEmpty())
    check(unchanged.revision == updated.revision)

    val deleted = manager.deleteTasks(listOf("read-docs", "ship"))
    check(deleted.affectedTasks.size == 2) // Contains removed records in request order.
    check(context.currentSnapshot().tasks.isEmpty())
}
```

**Observe and scope** — the supplied scope owns collection and error handling:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot
import org.orev.nahidka.feature.tasks.service.TasksRepository

fun observeTasks(
    repository: TasksRepository,
    scope: CoroutineScope,
    render: (TasksSnapshot) -> Unit,
): Job = scope.launch {
    repository.tasksState.collect { snapshot -> render(snapshot) }
}
// StateFlow immediately supplies current state; slow collectors may skip revisions.
// Cancel the returned Job or its scope when the owner ends.
// Snapshots have structural equality; sort explicitly for UI ordering.
// Compose consumer (with Compose runtime):
// val snapshot by context.tasksState.collectAsState()
// val sortedTasks = snapshot.tasks.sortedByDescending { it.priority }
```

**Metro DI** — consumer needs the Metro plugin and `implementation(libs.metro.runtime)`:

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.tasks.di.TasksSessionGraph

fun tasksSession(): TasksSessionGraph = createGraph<TasksSessionGraph>()
// Graph exposes tasksContext + tasksManager; TasksRepository binds to tasksContext.
// TasksSessionScope scopes one default-empty context and manager per graph.
// Retain the graph for the session. Use direct construction to seed initialTasks.
// There is no close()/dispose() API; the caller owns collector cancellation.
// App.kt uses remember(session) { TasksContext() } and remembers its manager.
// Recreating the context starts with the supplied initialTasks at revision 0.
```

Source: [repository](src/commonMain/kotlin/org/orev/nahidka/feature/tasks/service/TasksRepository.kt), [context](src/commonMain/kotlin/org/orev/nahidka/feature/tasks/service/TasksContext.kt), [DTOs](src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto), [app usage](../../../src/commonMain/kotlin/org/orev/nahidka/App.kt), [UI smoke test](../../../src/jvmTest/kotlin/org/orev/nahidka/ReviewFeaturesUiTest.kt).
