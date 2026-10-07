# Social battery

One optional percentage in memory, within `SocialBattery.PERCENTAGE_RANGE` (`0..100`). Read through `SocialBatteryContext`; write through `SocialBatteryManager`.

```kotlin
// Consumer build.gradle.kts, commonMain.dependencies:
implementation(project(":shared:core:feature:social-battery"))
// Targets: Android, JVM (11), JS browser, Wasm JS browser, iOS arm64/simulator arm64.
// Exposes core:common + kotlinx.coroutines; no storage, network, or UI dependency.
```

**Create, observe, update** — self-contained example.

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.socialbattery.dto.*
import org.orev.nahidka.feature.socialbattery.service.*

suspend fun batteryExample(ownerScope: CoroutineScope) {
    val context = SocialBatteryContext()
    val manager = SocialBatteryManager(context)
    check(context.currentSnapshot() == SocialBatterySnapshot(0, null))

    val collector = ownerScope.launch {
        context.batteryState.collect { snapshot ->
            val label = snapshot.socialBattery?.let { "${it.percentage}%" } ?: "Not set"
            println("${snapshot.revision}: $label")
        }
    }

    val updated = manager.updateBattery(SocialBattery(80))
    check(updated == SocialBatteryMutationResult(1, SocialBattery(80), true))
    val same = manager.updateBattery(SocialBattery(80))
    check(!same.changed && same.revision == 1L)
    manager.updateBattery(SocialBattery(0)) // Explicitly empty; differs from an unset value.
    check(context.currentSnapshot().revision == 2L)

    // Caller-managed restoration creates a NEW context; revision always starts at zero.
    val restored = SocialBatteryContext(initialSocialBattery = SocialBattery(80))
    check(restored.currentSnapshot() == SocialBatterySnapshot(0, SocialBattery(80)))
    collector.cancel() // Or let the owning lifecycle cancel ownerScope.
}
```

**Metro alternative** — consumer needs the Metro plugin and `implementation(libs.metro.runtime)`.

```kotlin
import dev.zacsweers.metro.createGraph
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionGraph

fun batterySession(): SocialBatterySessionGraph = createGraph<SocialBatterySessionGraph>()

// graph.socialBatteryContext: SocialBatteryContext
// graph.socialBatteryManager: SocialBatteryManager
// SocialBatterySessionScope is the Metro scope marker: one context/manager per graph.
// Own graphs reuse the providers: @DependencyGraph(SocialBatterySessionScope::class, bindingContainers = [SocialBatteryBindings::class]).
// Graph always starts with null; use the direct constructor to seed an initial value.
// Retain the graph for the session. There is no close() or owned coroutine scope.
```

Sources: [service implementations](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/service), [Metro bindings](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatteryBindings.kt), [Metro graph](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatterySessionGraph.kt), [App wiring](../../../src/commonMain/kotlin/org/orev/nahidka/App.kt), [Social battery UI](../../../ui/social-battery/README.md), [App integration test](../../../src/jvmTest/kotlin/org/orev/nahidka/SocialBatteryHostTest.kt).
