# Goals

In-memory goal CRUD with immutable snapshots. A goal has a title, a description, an optional picture (an emoji or photo bytes), progress from 0 to 100 % and an optional deadline date. Each context owns an independent session; callers supply identifiers and persistence.

```kotlin
// Consumer build.gradle.kts — Android / JVM 11 / iOS arm64 + simulator arm64 / JS / Wasm JS
kotlin.sourceSets.commonMain.dependencies {
    implementation(project(":shared:core:feature:goals"))
}
```

**Create, update, delete** — self-contained usage:

```kotlin
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.goals.dto.*
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager

suspend fun goalExample() {
    val context = GoalsContext() // Or GoalsContext(initialGoals = listOf(GoalRecord(...))).
    val manager = GoalsManager(context)
    check(context.currentSnapshot().revision == 0L)

    val created = manager.createGoal(GoalCreationRequest(
        identifier = "learn-kotlin", title = "Learn Kotlin",
        picture = GoalPicture.Emoji("📚"),
        // Defaults: description = "", progressPercentage = 0f, deadlineDate = null.
    ))
    check(created.changed && created.revision == 1L)

    val updated = manager.updateGoal(GoalUpdateRequest(
        identifier = created.goal.identifier,
        title = "Ship a Kotlin app",
        description = "Publish it to the store",
        picturePatch = NullablePatch.Set(GoalPicture.Photo(byteArrayOf(1, 2, 3))),
        progressPercentage = 25f,
        deadlinePatch = NullablePatch.Set(LocalDate(2027, 1, 1)),
    ))
    check(updated.goal.progressPercentage == 25f)
    check(updated.goal.picture == GoalPicture.Photo(byteArrayOf(1, 2, 3))) // Photos compare by content.

    manager.updateGoal(GoalUpdateRequest("learn-kotlin", deadlinePatch = NullablePatch.Clear))
    val unchanged = manager.updateGoal(GoalUpdateRequest("learn-kotlin"))
    check(!unchanged.changed) // Same revision; no state publication.

    val deleted = manager.deleteGoal("learn-kotlin")
    check(deleted.goal.identifier == "learn-kotlin") // Returns the removed record.
    check(context.currentSnapshot().goals.isEmpty())
}
```

Rejected with `IllegalArgumentException`: a blank identifier or title, a duplicate or unknown identifier, progress outside `GoalRecord.PROGRESS_PERCENTAGE_RANGE`, a blank emoji and an empty photo.

**Observe and scope** — the supplied scope owns collection and error handling:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.service.GoalsRepository

fun observeGoals(
    repository: GoalsRepository,
    scope: CoroutineScope,
    render: (GoalsSnapshot) -> Unit,
): Job = scope.launch {
    repository.goalsState.collect { snapshot -> render(snapshot) }
}
// StateFlow immediately supplies current state; slow collectors may skip revisions.
// Cancel the returned Job or its scope when the owner ends.
```

**Metro DI** — consumer needs the Metro plugin and `implementation(libs.metro.runtime)`:

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.goals.di.GoalsSessionGraph

fun goalsSession(): GoalsSessionGraph = createGraph<GoalsSessionGraph>()
// Graph exposes goalsContext + goalsManager; GoalsRepository binds to goalsContext.
// GoalsBindings holds those providers, so other graphs (GoalsScreenGraph in shared/ui/goal) reuse them.
// GoalsSessionScope scopes one default-empty context and manager per graph.
// Retain the graph for the session. Use direct construction to seed initialGoals.
// There is no close()/dispose() API; the caller owns collector cancellation.
```

Source: [repository](src/commonMain/kotlin/org/orev/nahidka/feature/goals/service/GoalsRepository.kt), [context](src/commonMain/kotlin/org/orev/nahidka/feature/goals/service/GoalsContext.kt), [DTOs](src/commonMain/kotlin/org/orev/nahidka/feature/goals/dto), [bindings](src/commonMain/kotlin/org/orev/nahidka/feature/goals/di/GoalsBindings.kt), [tests](src/commonTest/kotlin/org/orev/nahidka/feature/goals/service/GoalsContextTest.kt), [UI](../../../ui/goal/README.md).
