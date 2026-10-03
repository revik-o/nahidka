# Tasks feature API

This is the usage guide for the API proposed in [`plan-tasks-manager.md`](../../../../../plan-tasks-manager.md). The feature currently contains only `TaskService`; the API below becomes available after that plan is implemented. The proposed first version stores tasks in memory for one user session. Tasks disappear when that context is discarded or the application restarts.

## Resolve the context and manager with Metro

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.tasks.TasksSessionGraph

val tasksSessionGraph = createGraph<TasksSessionGraph>()
val tasksContext = tasksSessionGraph.tasksContext
val tasksManager = tasksSessionGraph.tasksManager
```

The project configures Metro for compile-time dependency injection. Its existing UI declarations use `@Inject` and `@Provides`; a runtime dependency graph has not yet been wired into the application. This proposed feature adds its own `TasksSessionGraph` and applies the existing Metro plugin/runtime aliases to the tasks module.

Metro generates dependency wiring during compilation and creates dependency instances at runtime. `TasksManager` uses constructor injection. The graph provides one `TasksContext` and one `TasksManager` per graph through `@SingleIn(TasksSessionScope::class)`. Its exposed context is the same instance injected into its manager. Every module declaring Metro bindings or calling `createGraph` must apply the Metro compiler plugin. See [Metro dependency graphs](https://zacsweers.github.io/metro/latest/dependency-graphs/) and [scopes](https://zacsweers.github.io/metro/latest/scopes/).

Keep one graph in the owner of the current user session. Give its context to readers and its manager to writers. Create the graph at the session boundary; repeated graph creation in screen code creates independent stores. When the user changes, cancel the old user's observation scope, release the old graph, and create a new graph. DI scoping does not cancel subscriptions automatically. The feature-owned session scope avoids a dependency from core code to the UI's `AppScope`.

Direct construction remains available for unit tests and explicitly independent collections, including `TasksContext(initialTasks)` for seeded fixtures. Production consumers should request the manager/context from the session graph or through injected constructors in a graph that includes those session dependencies. Do not annotate `initialTasks` as an injected dependency; it is fixture data, and the production provider creates an empty context.

`TaskRecord`, `TaskCreationRequest`, `TaskUpdateRequest`, and `TaskStatus` belong to `org.orev.nahidka.feature.tasks`. They are separate from the placeholder transport model `org.orev.nahidka.api.Task` and UI model `org.orev.nahidka.ui.models.TaskEntity`.

Each top-level class, data class, enum, interface, exception, and scope marker has its own Kotlin file named after the type. This includes `TasksSubscriptionCallbacks.kt`, `TasksSubscriptionBuilder.kt`, `TasksSubscription.kt`, `TasksSessionScope.kt`, and `TasksSessionGraph.kt`. Companion objects remain nested within their owning types. The plan provides the complete 19-file production layout and a separate code block for every file.

## Subscribe and change tasks

This complete example starts observation, creates two tasks in one call, updates a status, removes both tasks in one call, waits for the deletion callback, and releases the subscription.

```kotlin
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.tasks.TaskCreationRequest
import org.orev.nahidka.feature.tasks.TaskStatus
import org.orev.nahidka.feature.tasks.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.TasksSessionGraph

suspend fun tasksManagerUsageExample() = coroutineScope {
    val tasksSessionGraph = createGraph<TasksSessionGraph>()
    val tasksContext = tasksSessionGraph.tasksContext
    val tasksManager = tasksSessionGraph.tasksManager
    val deletionObserved = CompletableDeferred<Unit>()

    val tasksSubscription = tasksContext.subscribe()
        .onSnapshot { snapshot ->
            println("Current tasks at revision ${snapshot.revision}: ${snapshot.tasks}")
        }
        .onUpdate { notification ->
            println("Updated tasks: ${notification.taskChanges}")
        }
        .onDelete { notification ->
            println("Deleted tasks: ${notification.tasks}")
            deletionObserved.complete(Unit)
        }
        .onInsert { notification ->
            println("Inserted tasks: ${notification.tasks}")
        }
        .onFailure { failure ->
            deletionObserved.completeExceptionally(failure)
        }
        .startIn(this)

    try {
        tasksManager.addNewTasks(
            TaskCreationRequest(taskIdentifier = "task-first", title = "Prepare implementation plan"),
            TaskCreationRequest(taskIdentifier = "task-second", title = "Review subscription behavior")
        )
        tasksManager.updateTasks(
            TaskUpdateRequest(taskIdentifier = "task-first", status = TaskStatus.DONE)
        )
        tasksManager.removeTasks("task-first", "task-second")
        deletionObserved.await()
    } finally {
        withContext(NonCancellable) {
            tasksSubscription.cancelAndJoin()
        }
    }
}
```

The fluent methods configure an immutable builder. Registration happens in the suspending `startIn(coroutineScope)` call. Always keep the returned builder when using separate statements; ignoring the result of `onInsert` or another configuration method discards that configuration. Chaining configures the same subscription description. Calling the same configuration method again replaces its handler. Starting a builder twice creates two subscriptions.

Successful `startIn` means registration completed and the initial snapshot was queued. It does not wait for the snapshot handler to finish. A handler can run before `startIn` returns. `onSnapshot` receives all existing tasks, including an empty list for a new context; existing tasks do not trigger `onInsert`. Provide `onSnapshot` when a reader needs a complete starting view.

Handlers run serially for each subscription, in the supplied scope's coroutine context. Different subscriptions can run concurrently. Handlers may suspend and call manager operations. Let a handler return before waiting for a later notification from that same subscription, because its next handler runs only after the current one finishes. Use a scope with the appropriate dispatcher for UI updates.

## Batch mutations

| Method | Arguments | Successful result |
| --- | --- | --- |
| `addNewTasks` | Vararg `TaskCreationRequest` | Created records in request order |
| `removeTasks` | Vararg task identifier strings | Removed records in request order |
| `updateTasks` | Vararg `TaskUpdateRequest` | Changed records in request order; unchanged records omitted |

All three methods suspend and return `TasksMutationResult`, with `revision` and `affectedTasks`. A changing batch advances the context revision once and queues one notification containing the whole batch. Returning from a mutation means the state was committed and delivery was attempted for registered subscribers. Callback processing happens asynchronously.

Task identifiers are supplied by the caller and must be nonblank and unique within the context. Titles must be nonblank. Description may be empty. The supported statuses are `TO_DO`, `IN_PROGRESS`, and `DONE`; this version permits transitions between any of them.

Duplicate identifiers within a batch, an insertion with an existing identifier, an update or deletion with an unknown identifier, and an invalid field throw `IllegalArgumentException`. The whole batch is rejected: state, revision, and notification queues remain unchanged. There is no arbitrary task-count limit in a batch; available memory still bounds the input size.

Empty calls, all-null patches for existing records, and equal-value updates return an empty `affectedTasks` list at the current revision and emit nothing. Unknown identifiers are rejected even when their update has no changed fields.

## Partial updates

Stored title, description, and status are nonnullable. A null patch field means leave that field unchanged. An empty description clears its text. Task identifiers cannot be changed through a patch.

```kotlin
import org.orev.nahidka.feature.tasks.TaskStatus
import org.orev.nahidka.feature.tasks.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.TasksManager

suspend fun updateTaskDetails(tasksManager: TasksManager) {
    tasksManager.updateTasks(
        TaskUpdateRequest(taskIdentifier = "task-first", status = TaskStatus.IN_PROGRESS),
        TaskUpdateRequest(taskIdentifier = "task-second", description = "")
    )
}
```

## Concurrent callers

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.orev.nahidka.feature.tasks.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.TasksManager

suspend fun updateTaskConcurrently(tasksManager: TasksManager) = coroutineScope {
    listOf(
        async {
            tasksManager.updateTasks(
                TaskUpdateRequest(taskIdentifier = "task-first", title = "Reviewed implementation plan")
            )
        },
        async {
            tasksManager.updateTasks(
                TaskUpdateRequest(taskIdentifier = "task-first", description = "Concurrency behavior reviewed")
            )
        }
    ).awaitAll()
}
```

Call these helpers after their referenced tasks exist. Mutations serialize through the context's coroutine `Mutex`. Each patch applies to the latest committed record, so concurrent patches to different fields both survive. For the same field, the later commit wins; coroutine launch order does not determine commit order. A concurrent deletion can make a subsequent update fail as an unknown identifier.

Cancellation observed before a commit prevents that commit. Cancellation racing with an already-running commit can leave committed state even if the caller does not obtain the result. Read the current snapshot to reconcile; cancellation does not undo committed changes.

## Read the latest snapshot

```kotlin
import org.orev.nahidka.feature.tasks.TasksContext
import org.orev.nahidka.feature.tasks.TasksSnapshot

suspend fun readCurrentTasks(tasksContext: TasksContext): TasksSnapshot {
    return tasksContext.currentSnapshot()
}
```

The snapshot is a consistent point-in-time read. Its tasks follow insertion order; updates preserve that order and removal followed by reinsertion places a task at the end. Lists in snapshots, notifications, and mutation results are defensive copies. Modifying a returned list does not modify the store or another subscriber's payload.

Snapshots carry the current context revision. Subsequent notifications for a healthy subscription have increasing revisions. Revisions are local to one context and reset to zero for a new context. A handler that reads `currentSnapshot()` can see a newer revision than its notification, since writers can continue while handlers run.

## Cancellation, failures, and slow subscribers

Keep the returned `TasksSubscription`. `cancel()` requests cancellation without waiting. `cancelAndJoin()` waits for the collector to stop and unregister; use it from the owning coroutine. Use `withContext(NonCancellable)` when awaiting cleanup from a canceled owner's `finally` block. From inside a subscription handler, including a coroutine it awaits, use `cancel()` and return. Canceling the observation scope also stops and unregisters its subscriptions. A currently-running handler must cooperate with coroutine cancellation.

`isActive` reports the collector job's activity. It can remain true while an overflowed subscription drains its queued notifications; it is not a delivery-health indicator.

Exceptions from handlers stop that subscription after cleanup and are passed to `onFailure`. Ordinary coroutine cancellation does not invoke `onFailure`. If no failure handler is configured, the exception is rethrown into the supplied scope. If `onFailure` itself throws, that exception also propagates. The caller's scope determines whether a failure cancels sibling coroutines; use a supervisor scope and a reporting handler when subscriptions need independent failure handling. Handler failure does not roll back task changes.

Each subscriber has a bounded FIFO queue. `subscribe(subscriptionBufferCapacity = 64)` sets its capacity in notifications, including the initial snapshot. A batch uses one queue slot regardless of its task count. Choose a larger positive bounded value for bursty workloads. Every notification type uses the queue even when its handler is omitted.

When a queue fills, that subscriber is removed and its channel is closed with `TasksSubscriptionOverflowException`. Already-queued notifications are delivered in order before `onFailure` receives the exception and its `firstMissedRevision`. Other subscribers and writers continue. If a handler remains suspended, the error is reported only after it returns and the queued prefix is processed; cancellation can stop that processing. Canceling a failed or slow subscription can therefore prevent its failure handler from running.

After overflow, report the failure and start a new subscription with `onSnapshot` to restore the latest complete state. This version has no durable event history or automatic reconnection. A successful mutation acknowledges local state, not delivery completion or backend persistence.

## Implementation validation

After applying the plan, run from `client`:

```sh
./gradlew :shared:core:feature:tasks:jvmTest :shared:core:feature:tasks:compileKotlinJs :shared:core:feature:tasks:compileKotlinWasmJs
```

The plan also specifies Android compilation and Apple simulator validation, deterministic cancellation and overflow tests, and a JVM test that exercises producers on real worker threads.
