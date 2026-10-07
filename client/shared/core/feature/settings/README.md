# Settings

Persisted theme/language preferences. Read through `SettingsContext`; write through `SettingsManager`.

```kotlin
// Consumer build.gradle.kts, commonMain.dependencies:
implementation(project(":shared:core:feature:settings"))
// Targets: Android, JVM (11), JS browser, Wasm JS browser, iOS arm64/simulator arm64.
// Exposes core:common + kotlinx.coroutines; UI and platform storage live outside this lib.
```

**Create, observe, save** — self-contained example; replace the in-memory string with durable storage.

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.settings.dto.*
import org.orev.nahidka.feature.settings.service.*

suspend fun settingsExample(ownerScope: CoroutineScope) {
    var encoded: String? = null
    val storage = KeyValueSettingsLocalDataSource(
        readValue = { encoded },
        writeValue = { encoded = it },
    )
    val context = SettingsContext(storage) // Loads synchronously; revision starts at 0.
    val manager = SettingsManager(context) // Keep the same context for all readers/writers.
    val collector = ownerScope.launch {
        context.settingsState.collect { snapshot ->
            println("${snapshot.revision}: ${snapshot.settings}")
        }
    }

    val saved = manager.saveSettings(
        context.currentSnapshot().settings.copy(
            theme = SettingsTheme.DARK,
            language = SettingsLanguage.UKRAINIAN,
        ),
    )
    check(saved.changed && saved.revision == 1L)
    check(encoded == "DARK|UKRAINIAN")
    check(!manager.saveSettings(saved.settings).changed) // No extra write/revision.

    val restored = SettingsContext(storage)
    check(restored.currentSnapshot() == SettingsSnapshot(0, saved.settings))
    collector.cancel() // Or let the owning lifecycle cancel ownerScope.
}
```

**Metro alternative** — consumer needs the Metro plugin and `implementation(libs.metro.runtime)`.

```kotlin
import dev.zacsweers.metro.createGraphFactory
import org.orev.nahidka.feature.settings.di.SettingsSessionGraph
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource

fun settingsSession(storage: SettingsLocalDataSource): SettingsSessionGraph =
    createGraphFactory<SettingsSessionGraph.Factory>().create(storage)

// graph.settingsContext: SettingsContext
// graph.settingsManager: SettingsManager
// Factory.create(localDataSource: SettingsLocalDataSource): SettingsSessionGraph
// SettingsBindings holds the context provider, so ApplicationSessionGraph in :shared reuses it.
// SettingsSessionScope is the Metro scope marker: one context/manager per graph.
// Retain the graph for the desired lifetime. There is no close() or owned coroutine scope.
```

Sources: [service implementations](src/commonMain/kotlin/org/orev/nahidka/feature/settings/service), [Metro graph](src/commonMain/kotlin/org/orev/nahidka/feature/settings/di/SettingsSessionGraph.kt), [bindings](src/commonMain/kotlin/org/orev/nahidka/feature/settings/di/SettingsBindings.kt), [App integration](../../../src/commonMain/kotlin/org/orev/nahidka/di/ApplicationSessionGraph.kt), [settings screen](../../../ui/settings/src/commonMain/kotlin/org/orev/nahidka/ui/settings/SettingsViewModel.kt).
