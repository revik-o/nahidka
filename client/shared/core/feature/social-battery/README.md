# Social battery feature API

This guide documents the implemented social battery API. [`plan-social-battery.md`](../../../../../plan-social-battery.md) records its design and behavior contracts.

The API holds one current user's battery in memory for one session. A new session starts with an unknown battery, represented by `null`. Data is lost when the context is discarded or the application restarts. Backend integration is outside this implementation, so the unfinished API stub is not called.

## Resolve one context and manager per session

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionGraph

val socialBatterySessionGraph = createGraph<SocialBatterySessionGraph>()
val socialBatteryContext = socialBatterySessionGraph.socialBatteryContext
val socialBatteryManager = socialBatterySessionGraph.socialBatteryManager
```

Keep this graph in the current user's session owner and share its context and manager with consumers. The manager writes to the same scoped context exposed by the graph. Creating another graph creates independent state. On user changes, cancel the old observation scopes, discard the old graph, and create a new graph. Metro scoping does not cancel coroutine jobs.

The feature module applies the existing Metro compiler plugin. Modules that call `createGraph` or declare new Metro bindings must also apply that plugin and depend on its runtime. Graph wiring is generated at compile time; graph instances are created at runtime. See [Metro dependency graphs](https://zacsweers.github.io/metro/latest/dependency-graphs/).

Direct construction is useful for tests and explicitly independent state:

```kotlin
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

val socialBatteryContext = SocialBatteryContext(initialSocialBattery = SocialBattery(60))
val socialBatteryManager = SocialBatteryManager(socialBatteryContext)
```

Use one setup approach in a given scope. Seeded state starts at revision zero and is delivered as a snapshot. The feature DTO `org.orev.nahidka.feature.socialbattery.dto.SocialBattery` is separate from the transport placeholder `org.orev.nahidka.api.SocialBattery`.

## Subscribe and update the battery

This complete example observes the initial state, inserts a value, updates it, waits for the update callback, and releases the subscription.

```kotlin
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionGraph
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

suspend fun socialBatteryUsageExample() = coroutineScope {
    val socialBatterySessionGraph = createGraph<SocialBatterySessionGraph>()
    val socialBatteryContext = socialBatterySessionGraph.socialBatteryContext
    val socialBatteryManager = socialBatterySessionGraph.socialBatteryManager
    val updateObserved = CompletableDeferred<Unit>()

    val socialBatterySubscription = socialBatteryContext.subscribe()
        .onSnapshot { snapshot ->
            println("Current battery at revision ${snapshot.revision}: ${snapshot.socialBattery}")
        }
        .onUpdate { notification ->
            println("Battery changed from ${notification.previousSocialBattery.percentage}% to ${notification.currentSocialBattery.percentage}%")
            updateObserved.complete(Unit)
        }
        .onInsert { notification ->
            println("First battery value: ${notification.socialBattery.percentage}%")
        }
        .onFailure { failure ->
            updateObserved.completeExceptionally(failure)
        }
        .startIn(this)

    try {
        val insertionResult = socialBatteryManager.updateBattery(SocialBattery(80))
        val updateResult = socialBatteryManager.updateBattery(SocialBattery(45))
        println("Committed revisions: ${insertionResult.revision}, ${updateResult.revision}")
        withTimeout(5_000) { updateObserved.await() }
    } finally {
        withContext(NonCancellable) {
            socialBatterySubscription.cancelAndJoin()
        }
    }
}
```

`subscribe()` and handler methods configure an immutable builder. The suspending `startIn(coroutineScope)` registers the subscription and queues its initial snapshot. A bare chain without `startIn` does not observe anything. Keep the returned builder when configuring in separate statements. Repeating a handler method replaces that handler. Starting one builder twice creates two independent subscriptions.

The observation scope must have an active lifecycle `Job`. An already-canceled scope or startup caller is rejected. Cancellation racing after registration can stop a successfully returned subscription; startup does not promise continuing activity. Handlers run serially per subscription in the supplied scope's coroutine context. Separate subscriptions can run concurrently. Choose the appropriate dispatcher for UI changes.

`startIn` does not wait for the snapshot callback to finish. Callbacks may run before it returns, so they must not access a handle that has not yet been assigned. Handlers may suspend, read the context, and call the manager. A handler must return before awaiting a later notification from its own serial subscription.

## Mutation and snapshot rules

`SocialBattery(percentage)` accepts whole percentages from zero through one hundred. Invalid values throw `IllegalArgumentException` at construction, before a manager call. Values are rejected rather than clamped.

| Existing state | Requested value | Result |
| --- | --- | --- |
| Unknown (`null`) | Any valid percentage, including zero | `onInsert`; revision advances once |
| Known | Different percentage | `onUpdate` with previous and current entities; revision advances once |
| Known | Same percentage | No notification; revision unchanged; `hasChanged = false` |

`updateBattery` suspends and returns `SocialBatteryMutationResult(revision, socialBattery, hasChanged)`. Returning acknowledges a local commit and notification delivery attempts. Callbacks execute asynchronously. The result does not acknowledge server persistence or callback completion.

`onSnapshot` supplies the complete initial state, including `null` when unknown. Existing state is not replayed as an insertion. Omitted handlers consume their notifications without doing anything. Notifications, results, and snapshots contain immutable entities.

```kotlin
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext

suspend fun readCurrentSocialBattery(
    socialBatteryContext: SocialBatteryContext
): SocialBatterySnapshot = socialBatteryContext.currentSnapshot()
```

`currentSnapshot()` reads state and revision consistently. Initial snapshot registration and writes share one coroutine mutex, so a racing write appears in the initial snapshot or a subsequent event. Healthy subscriptions receive events in commit order with increasing revisions. A callback that reads a snapshot can see newer state than its own notification.

## Concurrent callers

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryMutationResult
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

suspend fun updateSocialBatteryConcurrently(
    socialBatteryManager: SocialBatteryManager
): List<SocialBatteryMutationResult> = coroutineScope {
    listOf(
        async { socialBatteryManager.updateBattery(SocialBattery(25)) },
        async { socialBatteryManager.updateBattery(SocialBattery(75)) }
    ).awaitAll()
}
```

Writes serialize through the context's mutex. The later commit wins; launch order does not determine commit order. Equality is checked against the latest committed value. Revisions are local to one context and reset to zero in a new context. They are not backend versions.

Cancellation observed before committing prevents the write. Cancellation racing after the final check can leave committed state even if the caller never receives a result. Read `currentSnapshot()` to reconcile; cancellation does not undo a commit.

For a widget that accepts a zero-to-one `Float`, convert a known percentage explicitly:

```kotlin
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

fun socialBatteryDisplayLevel(socialBattery: SocialBattery): Float =
    socialBattery.percentage / 100f
```

The UI should represent unknown state separately instead of interpreting it as zero percent. Wiring the existing dashboard to this feature is a separate UI change.

## Cancellation and failures

Keep the returned `SocialBatterySubscription`. `cancel()` requests cancellation; `cancelAndJoin()` also waits for collector cleanup. Use `cancelAndJoin()` from the subscription's owner. In a canceled owner's `finally`, use `withContext(NonCancellable)` as in the example. Inside a callback or a child coroutine it awaits, use `cancel()` and return. The ownership guard rejects joining from the collector's inherited context. Owner-scope cancellation also stops its subscriptions. Handlers must cooperate with coroutine cancellation.

`isActive` reports the collector job's activity. It may remain true while an overflowed stream drains; it does not promise delivery health.

Callback failures stop and unregister that subscription before `onFailure` runs. Cancellation does not invoke `onFailure`. Without a configured failure handler, the exception is rethrown into the observation scope. A failure handler that throws also propagates. The scope's supervision policy determines effects on sibling coroutines. Use a supervised scope and a reporting failure handler when subscriptions should fail independently. Failure does not roll back committed battery updates.

## Slow subscribers and recovery

`subscribe(subscriptionBufferCapacity = 64)` sets a positive bounded FIFO capacity in notifications. The initial snapshot uses a slot. Every event consumes a slot even when its handler is omitted. Publication uses non-suspending sends so slow callbacks do not make writers await callback processing. See [Kotlin channel trySend](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-send-channel/try-send.html).

A full queue removes only that subscriber and closes its channel with `SocialBatterySubscriptionOverflowException(firstMissedRevision)`. Its queued prefix drains in order before `onFailure` receives the exception. A suspended callback delays that report, and cancellation can prevent it. Other subscribers continue according to their own queue capacity. See [Kotlin channel close](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-send-channel/close.html).

After overflow, report the failure and create a new subscription with `onSnapshot` to restore the latest complete state. There is no automatic reconnect or durable event history. The plan includes a deterministic recovery test.

## Planned validation

After implementing the plan, run from `client`:

```sh
./gradlew :shared:core:feature:social-battery:jvmTest :shared:core:feature:social-battery:compileKotlinJs :shared:core:feature:social-battery:compileKotlinWasmJs
```

The plan supplies complete proposed sources, old/new changes, tests, Android/browser/Apple build gates, and a separately labeled record of any temporary JVM checks performed during planning.
