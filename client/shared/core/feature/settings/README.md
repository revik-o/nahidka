# Settings client API

This guide describes the settings core API implemented in this module. The detailed design and acceptance matrix are recorded in [plan-settings.md](../../../../../plan-settings.md).

One `SettingsContext` stores one immutable theme/language preference pair in memory. `SettingsManager.saveSettings` replaces the pair and notifies active subscribers of changes. The API does not currently persist preferences across application restarts or call a backend.

## Module dependency and compile-time DI

Consumers depend on `:shared:core:feature:settings`. Modules that declare Metro graphs or call `createGraph` apply the existing Metro plugin. Use the existing aliases:

```kotlin
plugins {
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:feature:settings"))
            implementation(libs.metro.runtime)
        }
    }
}
```

This is a Gradle configuration fragment to merge into the consumer's existing build file. The shared aggregate will export the feature, so modules depending on `:shared` can use its API transitively.

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.settings.di.SettingsSessionGraph

fun createSettingsSession(): SettingsSessionGraph = createGraph<SettingsSessionGraph>()
```

Create and retain one graph at the owning application/session boundary. Pass its `settingsContext` and `settingsManager` to consumers. Repeated graph access returns the same context and manager; another graph owns independent settings. Cancel old observation scopes before discarding a graph. Metro does not cancel those scopes for you.

## Theme and language values

`Settings()` starts with `SettingsTheme.FOLLOW_SYSTEM` and `SettingsLanguage.FOLLOW_SYSTEM`. A context can also be explicitly seeded with `SettingsContext(initialSettings)`; its revision starts at zero.

| Preference | Allowed values |
| --- | --- |
| Theme | `FOLLOW_SYSTEM`, `LIGHT`, `DARK` |
| Language | `FOLLOW_SYSTEM`, `ENGLISH`, `UKRAINIAN`, `RUSSIAN` |

Explicit languages correspond to the current default/English, `uk`, and `ru` resource directories. The enums represent preferences; the feature does not choose palettes, read system locale, or change Compose resources. Consumers resolve `FOLLOW_SYSTEM` through platform state and choose a supported fallback if needed.

## Subscribe, save, and clean up

This complete example creates one graph, starts observation, saves a full settings entity, waits for the update callback, and cleans up the subscription. The sample failure callback fails the waiting owner; a production consumer can instead report the failure and recover from a new snapshot.

```kotlin
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.settings.di.SettingsSessionGraph
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsLanguage
import org.orev.nahidka.feature.settings.dto.SettingsSnapshot
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.dto.SettingsUpdated

suspend fun saveSettingsAndObserve(): SettingsSnapshot = coroutineScope {
    val settingsSessionGraph = createGraph<SettingsSessionGraph>()
    val settingsContext = settingsSessionGraph.settingsContext
    val settingsManager = settingsSessionGraph.settingsManager
    val requestedSettings = Settings(
        theme = SettingsTheme.DARK,
        language = SettingsLanguage.UKRAINIAN
    )
    val updateReceived = CompletableDeferred<SettingsUpdated>()
    val settingsSubscription = settingsContext.subscribe()
        .onSnapshot { settingsSnapshot ->
            check(settingsSnapshot.revision == 0L)
        }
        .onUpdate { settingsUpdated ->
            updateReceived.complete(settingsUpdated)
        }
        .onFailure { failure ->
            updateReceived.completeExceptionally(failure)
        }
        .startIn(this)

    try {
        val settingsSaveResult = settingsManager.saveSettings(requestedSettings)
        val settingsUpdated = updateReceived.await()
        check(settingsSaveResult.hasChanged)
        check(settingsUpdated.currentSettings == requestedSettings)
        check(settingsUpdated.revision == settingsSaveResult.revision)
        settingsContext.currentSnapshot()
    } finally {
        withContext(NonCancellable) {
            settingsSubscription.cancelAndJoin()
        }
    }
}
```

`subscribe()` creates an immutable builder. Each `onSnapshot`, `onUpdate`, or `onFailure` returns a new builder; chain the result or retain it. Reconfiguring a handler replaces it in that builder. `startIn(scope)` activates the subscription and returns its handle. Starting one builder twice makes two independent subscriptions. The supplied scope must contain an active lifecycle `Job`.

Successful startup means registration is complete and the initial snapshot is queued. It does not wait for snapshot processing. A callback can run before `startIn` returns, so an initial callback must not access a subscription handle that has not yet been assigned.

`onSnapshot` receives current settings first. `onUpdate` receives later changed saves with previous/current settings and their revision. The first changed save is also an update because the context always has initial settings. There is no insertion event. Omitting a handler still consumes its notifications.

Handlers run serially within a subscription in the supplied scope's context. Different subscriptions can run concurrently. Handlers may suspend, read the context, or save settings. A handler must return before waiting for a later notification from its own subscription. A snapshot read inside a callback can be newer than that callback's event.

## Read or replace settings

```kotlin
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsSaveResult
import org.orev.nahidka.feature.settings.dto.SettingsSnapshot
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsManager

suspend fun readSettings(settingsContext: SettingsContext): SettingsSnapshot {
    return settingsContext.currentSnapshot()
}

suspend fun replaceSettings(
    settingsManager: SettingsManager,
    requestedSettings: Settings
): SettingsSaveResult {
    return settingsManager.saveSettings(requestedSettings)
}
```

`saveSettings` suspends and atomically replaces both fields. A changed save advances the local `Long` revision once and attempts notification delivery. An equal save returns `hasChanged = false` with the existing revision and emits nothing. Returning from a save does not wait for callback completion and does not acknowledge remote or disk persistence.

The enums prevent invalid arbitrary strings in the feature entity. Revisions start at zero, belong to one context, and do not wrap at `Long.MAX_VALUE`. A changed save at that limit throws without changing state; equal saves still return normally.

## Concurrent saves

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsLanguage
import org.orev.nahidka.feature.settings.dto.SettingsSaveResult
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.service.SettingsManager

suspend fun saveSettingsConcurrently(
    settingsManager: SettingsManager
): List<SettingsSaveResult> = coroutineScope {
    listOf(
        async {
            settingsManager.saveSettings(
                Settings(SettingsTheme.LIGHT, SettingsLanguage.ENGLISH)
            )
        },
        async {
            settingsManager.saveSettings(
                Settings(SettingsTheme.DARK, SettingsLanguage.UKRAINIAN)
            )
        }
    ).awaitAll()
}
```

All state reads, registration, and saves serialize through one coroutine mutex. Coroutine launch order does not determine commit order. The final entity is the complete pair from the later commit. Deriving two copies from one old snapshot and saving them can overwrite another caller's field change; full replacement does not merge stale edits. Use one owner to coordinate such edits, or define a future atomic patch/version precondition API before independent editors rely on merging.

Cancellation observed before commit prevents a save. Cancellation racing after the final check can leave committed settings even if the caller never receives its result. Reconcile through `currentSnapshot()`; cancellation does not undo a committed save.

## Lifecycle and failures

Keep the returned `SettingsSubscription`. `cancel()` requests cancellation and can be called from a handler. `cancelAndJoin()` waits for cleanup and belongs to the owner. Calling it from that subscription's callback or an inherited child coroutine throws to prevent waiting for its own termination. Canceling the observation scope also cancels its subscriptions. Use `withContext(NonCancellable)` for cleanup from a canceled owner's `finally` block. Handlers must cooperate with cancellation.

Callback failure stops and unregisters the subscription before `onFailure` runs. Ordinary coroutine cancellation bypasses `onFailure`. Its default implementation rethrows into the observation scope; a throwing failure handler does too. The scope's supervision policy determines sibling cancellation. Use a lifecycle-owned `SupervisorJob` and a reporting failure handler when subscriptions should fail independently. Failures do not roll back saved settings.

## Slow subscribers and recovery

`subscribe(subscriptionBufferCapacity = 64)` creates a bounded FIFO queue. Capacity must be positive and less than `Channel.UNLIMITED`; the initial snapshot counts toward it. Healthy subscribers receive changed revisions in commit order.

If a queue fills, that subscriber is removed and its queue closes with `SettingsSubscriptionOverflowException(firstMissedRevision)`. Accepted notifications drain in order before `onFailure`; a suspended handler delays the report, and cancellation can prevent it. Writers and other registered subscribers continue.

After overflow, start a new subscription with `onSnapshot` to apply current state. There is no automatic reconnect or durable event history. `isActive` reflects the collector job and can remain true while an overflowed queue drains.

The join guard uses inherited coroutine context. Preserve that context in callback descendants and avoid detached callback work that outlives the owner. Arbitrary external coroutine wait cycles still require caller discipline.

## Verification

The core acceptance suites passed on JVM, JS browser, and Wasm browser. Shared, web, desktop, and Android debug builds passed, and the feature compiled for iOS arm64 and iOS simulator arm64. Simulator runtime tests were not run on this Linux host because they require macOS.
