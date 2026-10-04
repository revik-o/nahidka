# Goals API

This guide documents the implemented client API described in [plan-goal.md](../../../../../plan-goal.md). The feature provides a session-scoped `GoalsContext`, a `GoalsManager`, and fluent subscriptions with compile-time Metro dependency injection.

The first implementation keeps goals in memory for one user session. A new session graph starts empty. Changes are not persisted across graph replacement or application restart. The unfinished `GoalsApi` backend stub is not called.

## Session setup and compile-time injection

The goals feature applies the project's Metro compiler plugin and runtime dependency. Any module calling `createGraph` or declaring an injected consumer must apply Metro and include its runtime dependency too.

Create one graph when the user session starts, and share its context and manager with all readers and writers for that session:

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.goals.di.GoalsSessionGraph

fun createGoalsSessionGraph(): GoalsSessionGraph = createGraph<GoalsSessionGraph>()
```

`GoalsManager` uses `@Inject` constructor injection. The graph provides one `GoalsContext` and one manager in `GoalsSessionScope`. Repeated access to one graph returns the same instances; different graphs have independent data. Graph wiring is generated and checked at compile time; graph instances are created at runtime. Retain the graph in the session owner instead of creating a graph for each operation or screen.

Subscriptions use a caller-owned coroutine scope containing an active lifecycle `Job`. Cancel the old observation scope and release the old graph when changing users. Metro scoping caches instances; it does not cancel coroutines. UI subscriptions should use the UI dispatcher supplied by their owner.

## Observe and change goals

The builder preserves the requested fluent shape. `startIn` activates the subscription after all handlers have been configured. `onSnapshot` supplies the current full list, including an empty list. Existing goals do not trigger artificial insert events.

This complete example starts an observer, creates, updates, and deletes one goal, and waits for the deletion handler before stopping. Pass a fresh graph to run it:

```kotlin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.goals.di.GoalsSessionGraph
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot

suspend fun exerciseGoalsApi(goalsSessionGraph: GoalsSessionGraph): GoalsSnapshot = supervisorScope {
    val goalsContext = goalsSessionGraph.goalsContext
    val goalsManager = goalsSessionGraph.goalsManager
    val visibleGoals = MutableStateFlow<List<GoalRecord>>(emptyList())
    val deletionProcessed = CompletableDeferred<Unit>()
    val goalsSubscription = goalsContext.subscribe()
        .onSnapshot { snapshot -> visibleGoals.value = snapshot.goals }
        .onUpdate { notification ->
            visibleGoals.value = visibleGoals.value.map { goal ->
                if (goal.goalIdentifier == notification.currentGoal.goalIdentifier) {
                    notification.currentGoal
                } else {
                    goal
                }
            }
        }
        .onInsert { notification -> visibleGoals.value = visibleGoals.value + notification.goal }
        .onDelete { notification ->
            visibleGoals.value = visibleGoals.value.filter { goal ->
                goal.goalIdentifier != notification.goal.goalIdentifier
            }
            if (notification.goal.goalIdentifier == "goal-kotlin") {
                deletionProcessed.complete(Unit)
            }
        }
        .onFailure { failure -> deletionProcessed.completeExceptionally(failure) }
        .startIn(this)

    try {
        goalsManager.createGoal(GoalCreationRequest("goal-kotlin", "Learn Kotlin"))
        goalsManager.updateGoal(GoalUpdateRequest("goal-kotlin", "Learn Kotlin coroutines"))
        goalsManager.deleteGoal("goal-kotlin")
        deletionProcessed.await()
        check(visibleGoals.value.none { goal -> goal.goalIdentifier == "goal-kotlin" })
        goalsContext.currentSnapshot()
    } finally {
        withContext(NonCancellable) {
            goalsSubscription.cancelAndJoin()
        }
    }
}
```

`subscribe()` only constructs an immutable builder. Handler methods return a new builder; retain their result or chain them. Configuring the same handler again replaces it. Starting the same builder twice creates two independent subscriptions.

Successful `startIn` means the subscriber is registered and its initial snapshot is queued. It does not wait for `onSnapshot` to finish. A callback can run before `startIn` returns, so avoid accessing a handle that has not yet been assigned from an initial handler.

Handlers run serially within one subscription, in its supplied coroutine context. Different subscriptions can run independently. Handlers may suspend and call manager methods. A handler must return before waiting for a later notification from its own subscription. Every event is consumed even if its handler is omitted.

## Mutation contract

| Method | Input | Successful result |
| --- | --- | --- |
| `createGoal` | `GoalCreationRequest(goalIdentifier, title)` | Created record, new revision, `changed = true` |
| `deleteGoal` | Goal identifier string | Removed record, new revision, `changed = true` |
| `updateGoal` | `GoalUpdateRequest(goalIdentifier, title)` | Current record, current/new revision, whether it changed |

Methods suspend and return `GoalsMutationResult(revision, goal, changed)`. Returning from a changing mutation means the local state was committed and notification delivery was attempted. Callback completion is asynchronous; a callback may also start before the mutation returns. The result does not acknowledge backend persistence.

Identifiers and titles must be nonblank. Identifiers are caller-supplied, unique within a context, and immutable. Values are preserved as supplied; the feature does not trim or normalize identifiers or titles. Inserting an existing identifier, updating/deleting an unknown identifier, or using an invalid field throws `IllegalArgumentException`, with no state, revision, or notification change.

The stored goal has only `goalIdentifier` and `title`, matching the current transport model's identity and title. UI preview progress and deadline fields are not part of this first contract.

## Nullable patches and no-ops

Null means leave the title unchanged. The stored title is nonnullable. Equal titles and all-null patches for an existing goal return `changed = false`, preserve the revision, and emit nothing. Unknown identifiers still fail for an all-null patch. A blank or empty title is invalid; it does not clear the title.

```kotlin
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult
import org.orev.nahidka.feature.goals.service.GoalsManager

suspend fun preserveGoalTitle(
    goalsManager: GoalsManager,
    goalIdentifier: String
): GoalsMutationResult {
    return goalsManager.updateGoal(GoalUpdateRequest(goalIdentifier = goalIdentifier))
}
```

Call this helper after that goal exists. If future stored fields can contain null, they will need explicit unchanged/set/clear patch values before those fields are added.

## Read current state

```kotlin
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.service.GoalsContext

suspend fun readCurrentGoals(goalsContext: GoalsContext): GoalsSnapshot {
    return goalsContext.currentSnapshot()
}
```

The snapshot is a consistent point-in-time read. Goals follow insertion order; updating preserves position and removing then recreating an identifier places it at the end. Snapshot lists are defensive copies and records contain immutable values. Changing a returned list cannot change the context or another subscriber's view.

Revisions start at zero, advance once for each changing mutation, and belong to one context. A new context starts over. Healthy subscriptions receive the initial revision followed by increasing mutation revisions. A callback reading `currentSnapshot()` can see a newer revision than its notification because writers can continue while handlers run.

## Concurrent callers

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult
import org.orev.nahidka.feature.goals.service.GoalsManager

suspend fun createGoalsConcurrently(goalsManager: GoalsManager): List<GoalsMutationResult> = coroutineScope {
    listOf(
        async { goalsManager.createGoal(GoalCreationRequest("goal-reading", "Read every week")) },
        async { goalsManager.createGoal(GoalCreationRequest("goal-running", "Run every week")) }
    ).awaitAll()
}
```

Run this helper only when those identifiers are unused. Mutations serialize with a coroutine `Mutex`; coroutine launch order does not determine commit order. An update patches the latest stored record under the mutex. Concurrent writes to the title use the later commit; a deletion can make a subsequent update fail as unknown. Two concurrent creations of one identifier produce one successful creation and one duplicate failure.

Cancellation observed before a commit prevents it. Cancellation racing after the final cancellation checkpoint can leave committed state even if the caller never receives the result. Reconcile with `currentSnapshot()`; cancellation does not undo a committed change.

## Lifecycle, failures, and overflow

Keep the returned `GoalsSubscription`. `cancel()` requests cancellation and is safe inside a callback. `cancelAndJoin()` waits for the collector to stop and unregister; call it from the owning coroutine. Calling it from that subscription's callback or an inherited child coroutine throws `IllegalStateException` to prevent waiting on its own termination. Use `withContext(NonCancellable)` when awaiting cleanup from a canceled owner's `finally` block. Canceling the observation scope also cancels its subscriptions. Handlers must cooperate with cancellation.

Callback exceptions stop and unregister that subscription before invoking `onFailure`. Ordinary coroutine cancellation does not invoke `onFailure`. Without an explicit failure handler, the exception is rethrown into the observation scope. A throwing `onFailure` also propagates. The scope's supervision policy determines whether sibling coroutines are canceled; use a supervisor and a reporting handler when observations must fail independently. Handler failure does not roll back committed mutations.

`subscribe(subscriptionBufferCapacity = 64)` uses a bounded FIFO queue per subscriber. The capacity counts notifications, including the initial snapshot. A slow subscriber can exhaust its queue. It is then removed and its channel closes with `GoalsSubscriptionOverflowException(firstMissedRevision)`. Its already-queued prefix drains in order before `onFailure`; a suspended handler delays that failure, and cancellation can prevent its delivery. Other subscribers and writers continue.

After overflow, start a new subscription with `onSnapshot` to rebuild the latest view. There is no automatic reconnection or durable event history. `isActive` reports the collector job's state; it may still be true while an overflowed queue drains and is not a delivery-health indicator.

The ownership guard relies on inherited coroutine context. Avoid removing that context marker or starting detached work from handlers; subscription ownership belongs to the caller's lifecycle.

## Implementation validation

The implementation and API examples passed these checks from `client`:

```sh
./gradlew :shared:core:feature:goals:jvmTest :shared:core:feature:goals:jsBrowserTest :shared:core:feature:goals:wasmJsBrowserTest
./gradlew :shared:core:feature:goals:compileKotlinJs :shared:core:feature:goals:compileKotlinWasmJs :desktopApp:compileKotlin :androidApp:assembleDebug
./gradlew :shared:compileKotlinJs :shared:compileKotlinWasmJs :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
./gradlew :shared:core:feature:goals:compileKotlinIosArm64 :shared:core:feature:goals:compileKotlinIosSimulatorArm64 -Pkotlin.native.ignoreDisabledTargets=true
```

The JVM suite covers concurrent worker mutations and observer startup, cancellation boundaries, overflow, lifecycle behavior, generated Metro graph identities, and the README examples. The iOS arm64 and simulator arm64 compile tasks passed. The iOS simulator test task is macOS-only and was disabled by Gradle on this Linux host; run `./gradlew :shared:core:feature:goals:iosSimulatorArm64Test` on macOS to execute that runtime gate.
