# Goals

In-memory goal CRUD with immutable snapshots. Each context owns an independent session; callers supply identifiers and persistence.

```kotlin
// Consumer build.gradle.kts — Android / JVM 11 / iOS arm64 + simulator arm64 / JS / Wasm JS
kotlin.sourceSets.commonMain.dependencies {
    implementation(project(":shared:core:feature:goals"))
}
```

**Create, update, delete** — self-contained usage:

```kotlin
import kotlin.time.Instant
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
        // Defaults: progressPercentage = 0f, deadlineInstant = null.
    ))
    check(created.changed && created.revision == 1L)

    val updated = manager.updateGoal(GoalUpdateRequest(
        identifier = created.goal.identifier,
        title = "Ship a Kotlin app",
        progressPercentage = 25f,
        deadlinePatch = NullablePatch.Set(Instant.parse("2027-01-01T00:00:00Z")),
    ))
    check(updated.goal.progressPercentage == 25f)

    manager.updateGoal(GoalUpdateRequest("learn-kotlin", deadlinePatch = NullablePatch.Clear))
    val unchanged = manager.updateGoal(GoalUpdateRequest("learn-kotlin"))
    check(!unchanged.changed) // Same revision; no state publication.

    val deleted = manager.deleteGoal("learn-kotlin")
    check(deleted.goal.identifier == "learn-kotlin") // Returns the removed record.
    check(context.currentSnapshot().goals.isEmpty())
}
```

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
// Compose consumer (with Compose runtime):
// val snapshot by context.goalsState.collectAsState()
// val sortedGoals = snapshot.goals.sortedBy { it.title }
```

**Metro DI** — consumer needs the Metro plugin and `implementation(libs.metro.runtime)`:

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.goals.di.GoalsSessionGraph

fun goalsSession(): GoalsSessionGraph = createGraph<GoalsSessionGraph>()
// Graph exposes goalsContext + goalsManager; GoalsRepository binds to goalsContext.
// GoalsSessionScope scopes one default-empty context and manager per graph.
// Retain the graph for the session. Use direct construction to seed initialGoals.
// There is no close()/dispose() API; the caller owns collector cancellation.
// App.kt uses remember(session) { GoalsContext() } and remembers its manager.
// Recreating the context starts with the supplied initialGoals at revision 0.
```

Source: [repository](src/commonMain/kotlin/org/orev/nahidka/feature/goals/service/GoalsRepository.kt), [context](src/commonMain/kotlin/org/orev/nahidka/feature/goals/service/GoalsContext.kt), [DTOs](src/commonMain/kotlin/org/orev/nahidka/feature/goals/dto), [app usage](../../../src/commonMain/kotlin/org/orev/nahidka/App.kt), [UI smoke test](../../../src/jvmTest/kotlin/org/orev/nahidka/ReviewFeaturesUiTest.kt).
