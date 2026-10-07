# Implementation plan — `shared/ui/social-battery`

**Status (2026-10-07):** plan only, so the repository is unchanged apart from this file and `plan-previews/`. Every code block below was written, compiled and tested in an isolated copy of the repository at `6df8e9d`. **36 / 36 tests pass on three consecutive forced runs** (12 new, 24 existing as a regression check). The app builds for Android, desktop, JS and Wasm. Previews were rendered from this exact code (§5).

---

## 1. What gets built (mockup → implementation)

| Mockup element | Implementation |
|---|---|
| Battery outline with its cap | Drawn on one `Canvas`: rounded casing, terminal (cap), and a fill whose height is the level. The fill animates smoothly to a new level |
| `90%` | Centered in the battery and sized from the battery width, so it fits at every size. It shows `—` while no level has been set (the core keeps "never set" separate from `0`) |
| Dots around the battery | **Animated particles:** 96 sparks drift outward from the battery edges and fade, over a soft glow. Their number follows the level: 90 % shows 90 % of them, an empty or unset battery shows none. They are seeded, so every launch and every test render looks the same |
| *(not drawn)* How to set the level | **Tap or drag inside the battery:** the finger's height is the level (top = 100 %, bottom = 0 %). Mouse click and drag work the same way. Screen readers get an adjustable slider |
| *(not drawn)* Feedback text | A caption under the battery: *Drained — time to recharge* (0–29 %), *Steady — something calm will do* (30–69 %), *Charged — ready to socialize* (70–100 %), or *How charged do you feel?* (unset). A hint follows: *Tap or drag the battery to set your level* |
| Colors | One color per level, taken from the theme: `error` for low, `secondary` for medium, `primary` for high. The fill, glow, particles and caption all use it. No new hex colors are added, and light and dark themes both work |
| SIDE MENU | App shell, outside this module (unchanged). The temporary host top bar gets a **Social battery** entry (Step 8), as Tasks did |

### Mobile adaptivity

The screen measures **its own size** (`BoxWithConstraints`), so a narrow desktop window or browser behaves like a phone. The battery is sized from both the available width **and** height, so it never needs scrolling. That matters on a landscape phone, where height is the limit.

| | Expanded (≥ 600 dp: desktop, web, tablets) | Compact (< 600 dp: phones) |
|---|---|---|
| Padding | 24 dp | 16 dp. Both come from `LayoutWidth.screenPadding` in `shared/ui/common`, now shared with Tasks |
| Battery | Up to 74 % of the free height and up to 60 % of the width, whichever is smaller; aspect ratio 0.54 | Same rule. Portrait phones are width-limited (≈ 197 dp wide at 360 dp), landscape phones height-limited |
| Particle field | 2.4 × the battery's width and 1.35 × its height, clipped to the free space | Same |
| Percentage | 20 % of the battery width, independent of the system font scale, so `100%` always fits inside | Same |
| Captions | Centered under the battery, grouped with it in the middle of the screen | Same; long Ukrainian and Russian hints wrap to two lines |
| Input | Mouse click and drag; touch on tablets | Tap and drag; drags are consumed so they don't scroll a parent |

Checked at 1200 × 800 (light and dark), and in Ukrainian (the longest strings) at 360 × 800 portrait and 800 × 360 landscape. No control was clipped.

---

## 2. Decisions — please confirm or redirect

1. **The battery itself is the control.** Your mockup has a single element, so I didn't add a slider. Tap or drag vertically inside the battery, and the pointer's height sets the level. Every distinct percentage reached during a drag is written to the core. The core is in memory and ignores repeated values, but each new value raises the revision. *If you'd rather write only when the finger lifts (useful once there's persistence), that's a small change in `BatteryGauge`.*
2. **Unset and `0 %` are different**, as in the core. An unset battery shows `—`, an empty casing and no particles, and asks *How charged do you feel?*.
3. **Three charge levels with 30 / 70 % thresholds.** The colors come from the theme (`error` / `secondary` / `primary`), so the screen follows the light and dark themes.
4. **The particle effect follows the level:** more charge means more sparks. They flow outward from the battery and loop seamlessly every 18 s, each at 1–4 × that speed. The animation runs only while the screen is shown. It ignores the system *reduce motion* setting, because Compose Multiplatform has no common API for it.
5. **Core additions (allowed by your brief):**
   * `SocialBattery.PERCENTAGE_RANGE` is the single source of `0..100`. Core validation, UI clamping and the accessibility step count all use it.
   * `SocialBatteryBindings` is a `@BindingContainer`. Metro rejects a graph extending another graph, so the core `SocialBatterySessionGraph` and the new `SocialBatteryScreenGraph` share one provider, as `TasksBindings` does.
   * `SocialBatteryManager` gets a class-level `@Inject`, which removes the existing Metro compiler warning.
6. **DRY in `shared/ui/common`:** `LayoutWidth` now carries `screenPadding` (16 / 24 dp). Tasks and the new screen read it instead of repeating `if (COMPACT) 16.dp else 24.dp`. `FinancialTabLayout` repeats the same rule through its own `FinancialLayoutWidth`. That moves when the pending financial plan revision adopts `ui:common` (§5).
7. **No error UI.** The UI clamps every value before it builds a `SocialBattery`, so the core's only rejection (out of range) can't happen. Adding a snackbar path would be dead code.
8. **The old `SocialBatteryWidget` is deleted.** Its only caller was the slider tab that this plan replaces.
9. **Host:** the battery gets its own top-bar entry, and `PersonalFeature` loses `SOCIAL_BATTERY`, as it lost `TASKS`. The ViewModel is retained by the app session, so the level survives navigation.
10. **Strings:** en / uk / ru, all translated.

**How your style rules are applied:** there are no comments in any source file, tests included. Names are full words, including every lambda parameter (no implicit `it`). Builder, `Modifier`, flow and test-finder chains put one call per line. `@Inject` is class-level, and each file holds one class or one composable family. Imports follow the IntelliJ layout (wildcards from 5 names, `kotlin.*` last). The new code adds no compiler warnings on any target and removes one existing warning.

---

## 3. Architecture

```
shared/core/feature/social-battery         (Step 1)
├── dto/      SocialBattery(+PERCENTAGE_RANGE) · SocialBatterySnapshot · SocialBatteryMutationResult
├── service/  SocialBatteryContext (state) · SocialBatteryManager (writes)
└── di/       SocialBatteryBindings (shared provider) · SocialBatterySessionGraph

shared/ui/common                           (Step 2)
└── layout/   LayoutWidth(+screenPadding)    COMPACT 16 dp | EXPANDED 24 dp, breakpoint 600 dp

shared/ui/social-battery
├── SocialBatteryScreen                    public entry point: padding, field, caption, hint
├── SocialBatteryViewModel                 batteryState (core StateFlow) + updateBattery
├── SocialBatteryScreenGraph               DI composition root (host and tests)
└── battery/
    ├── BatteryField                       sizes the battery from width and height, stacks particles behind it
    ├── BatteryGauge                       Canvas: casing, terminal, fill, percentage; tap/drag; slider semantics
    ├── BatteryGeometry                    rectangles for drawing and pointer → level
    ├── BatteryParticles · BatteryParticle animated spark field
    ├── BatteryCharge                      LOW | MEDIUM | HIGH → threshold, caption, color
    └── BatteryChargeFraction              SocialBattery ⇄ 0f..1f
```

**Data flow**

* **Read:** `SocialBatteryContext.batteryState` → `SocialBatteryViewModel.batteryState` (the same `StateFlow`; nothing is copied) → `SocialBatteryScreen`. `BatteryCharge.of` supplies the color and caption, and `chargeFraction()` (animated) drives the gauge and the particles.
* **Write:** pointer position → `BatteryGeometry.chargeFractionAt`, or the screen reader's `setProgress` → `socialBatteryOf(fraction)` → `SocialBatteryViewModel.updateBattery` → `SocialBatteryManager.updateBattery`. The screen updates from the core state; nothing refreshes by hand.

**DRY map**

| Shared piece | Used by |
|---|---|
| `SocialBattery.PERCENTAGE_RANGE` (core) | core validation, `socialBatteryOf` clamping, `chargeFraction`, accessibility step count |
| `SocialBatteryBindings` (core) | `SocialBatterySessionGraph`, `SocialBatteryScreenGraph` (production and tests) |
| `LayoutWidth.screenPadding` (`ui:common`) | `TasksScreen`, `SocialBatteryScreen` |
| `chargeFraction()` / `socialBatteryOf()` | fill height, particle count, pointer input, accessibility value and action |
| `BatteryGeometry` | drawing the battery, mapping a touch to a level, centering the percentage |
| `BatteryCharge` (threshold + caption + color) | caption text, caption color, fill, glow, particles |
| `feature_socialbattery_title` (existing string) | the battery's accessibility label |

---

## 4. Implementation steps

Each file shows **Old** (empty for a new file) and **New**. Small or rewritten files show the whole file; larger files show only the changed regions, with two lines of context.

### Step 1 — Core: range constant, shared Metro bindings, warning fix (`shared/core/feature/social-battery`)

The core already does everything the screen needs (one optional percentage, revisioned `StateFlow`, deduplicated writes). Three small additions make it reusable without copies.

#### `shared/core/feature/social-battery/src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/dto/SocialBattery.kt` — modified

`PERCENTAGE_RANGE` becomes the single source of `0..100`: core validation, UI clamping and the accessibility step count all read it.

**Old:**

```kotlin
package org.orev.nahidka.feature.socialbattery.dto

data class SocialBattery(val percentage: Int) {
    init {
        require(percentage in 0..100) { "Social battery percentage must be between 0 and 100" }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.socialbattery.dto

data class SocialBattery(val percentage: Int) {
    init {
        require(percentage in PERCENTAGE_RANGE) {
            "Social battery percentage must be between ${PERCENTAGE_RANGE.first} and ${PERCENTAGE_RANGE.last}"
        }
    }

    companion object {
        val PERCENTAGE_RANGE = 0..100
    }
}
```

#### `shared/core/feature/social-battery/src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/service/SocialBatteryManager.kt` — modified

Class-level `@Inject`, like `TasksManager`. Removes the existing Metro warning *“There is only one @Inject-annotated constructor”*.

**Old:**

```kotlin
package org.orev.nahidka.feature.socialbattery.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionScope
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryMutationResult

@SingleIn(SocialBatterySessionScope::class)
class SocialBatteryManager @Inject constructor(private val socialBatteryContext: SocialBatteryContext) {

    suspend fun updateBattery(socialBattery: SocialBattery): SocialBatteryMutationResult =
        socialBatteryContext.applyBatteryUpdate(socialBattery)
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.socialbattery.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionScope
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryMutationResult

@Inject
@SingleIn(SocialBatterySessionScope::class)
class SocialBatteryManager(private val socialBatteryContext: SocialBatteryContext) {

    suspend fun updateBattery(socialBattery: SocialBattery): SocialBatteryMutationResult =
        socialBatteryContext.applyBatteryUpdate(socialBattery)
}
```

#### `shared/core/feature/social-battery/src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatteryBindings.kt` — new

The context provider moves out of the graph's companion into a `@BindingContainer`, because Metro rejects a graph extending another graph (same fix as `TasksBindings`).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.socialbattery.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext

@BindingContainer
object SocialBatteryBindings {

    @Provides
    @SingleIn(SocialBatterySessionScope::class)
    private fun provideSocialBatteryContext(): SocialBatteryContext = SocialBatteryContext()
}
```

#### `shared/core/feature/social-battery/src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatterySessionGraph.kt` — modified

Uses the shared bindings; public API (`socialBatteryContext`, `socialBatteryManager`) unchanged.

**Old:**

```kotlin
package org.orev.nahidka.feature.socialbattery.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

@DependencyGraph(SocialBatterySessionScope::class)
interface SocialBatterySessionGraph {
    val socialBatteryContext: SocialBatteryContext
    val socialBatteryManager: SocialBatteryManager

    companion object {
        @Provides
        @SingleIn(SocialBatterySessionScope::class)
        private fun provideSocialBatteryContext(): SocialBatteryContext = SocialBatteryContext()
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.socialbattery.di

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

@DependencyGraph(SocialBatterySessionScope::class, bindingContainers = [SocialBatteryBindings::class])
interface SocialBatterySessionGraph {
    val socialBatteryContext: SocialBatteryContext
    val socialBatteryManager: SocialBatteryManager
}
```

#### `shared/core/feature/social-battery/src/commonTest/kotlin/org/orev/nahidka/feature/socialbattery/dto/SocialBatteryTest.kt` — new

First test in this module: the range and its validation.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.socialbattery.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SocialBatteryTest {

    @Test
    fun percentageMustStayWithinRange() {
        assertEquals(0..100, SocialBattery.PERCENTAGE_RANGE)
        assertEquals(100, SocialBattery(100).percentage)
        assertFailsWith<IllegalArgumentException> { SocialBattery(-1) }
        assertFailsWith<IllegalArgumentException> { SocialBattery(101) }
    }
}
```

#### `shared/core/feature/social-battery/README.md` — modified

Documents the range and the bindings; replaces two dead links (`PersonalFeaturesScreen` no longer converts percentages, `ReviewFeaturesUiTest.kt` does not exist).

_Change 1 of 2 (old line 1 / new line 1)_

**Old:**

````markdown
# Social battery

One optional percentage in memory. Read through `SocialBatteryContext`; write through `SocialBatteryManager`.

```kotlin
````

**New:**

````markdown
# Social battery

One optional percentage in memory, within `SocialBattery.PERCENTAGE_RANGE` (`0..100`). Read through `SocialBatteryContext`; write through `SocialBatteryManager`.

```kotlin
````

_Change 2 of 2 (old line 56 / new line 56)_

**Old:**

````markdown
// graph.socialBatteryManager: SocialBatteryManager
// SocialBatterySessionScope is the Metro scope marker: one context/manager per graph.
// Graph always starts with null; use the direct constructor to seed an initial value.
// Retain the graph for the session. There is no close() or owned coroutine scope.
```

Sources: [service implementations](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/service), [Metro graph](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatterySessionGraph.kt), [App wiring](../../../src/commonMain/kotlin/org/orev/nahidka/App.kt), [UI percentage conversion](../../../src/commonMain/kotlin/org/orev/nahidka/PersonalFeaturesScreen.kt), [UI integration test](../../../src/jvmTest/kotlin/org/orev/nahidka/ReviewFeaturesUiTest.kt).
````

**New:**

````markdown
// graph.socialBatteryManager: SocialBatteryManager
// SocialBatterySessionScope is the Metro scope marker: one context/manager per graph.
// Own graphs reuse the providers: @DependencyGraph(SocialBatterySessionScope::class, bindingContainers = [SocialBatteryBindings::class]).
// Graph always starts with null; use the direct constructor to seed an initial value.
// Retain the graph for the session. There is no close() or owned coroutine scope.
```

Sources: [service implementations](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/service), [Metro bindings](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatteryBindings.kt), [Metro graph](src/commonMain/kotlin/org/orev/nahidka/feature/socialbattery/di/SocialBatterySessionGraph.kt), [App wiring](../../../src/commonMain/kotlin/org/orev/nahidka/App.kt), [Social battery UI](../../../ui/social-battery/README.md), [App integration test](../../../src/jvmTest/kotlin/org/orev/nahidka/SocialBatteryHostTest.kt).
````

### Step 2 — Shared UI: one screen padding rule (`shared/ui/common`, `shared/ui/tasks`)

`if (layoutWidth == LayoutWidth.COMPACT) 16.dp else 24.dp` would otherwise be copied into the new screen. It moves onto `LayoutWidth` itself.

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/layout/LayoutWidth.kt` — modified

Each width class carries its screen padding.

**Old:**

```kotlin
package org.orev.nahidka.ui.common.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val COMPACT_LAYOUT_WIDTH_LIMIT = 600.dp

enum class LayoutWidth {
    COMPACT,
    EXPANDED;

    companion object {

        fun of(availableWidth: Dp): LayoutWidth =
            if (availableWidth < COMPACT_LAYOUT_WIDTH_LIMIT) COMPACT else EXPANDED
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.common.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val COMPACT_LAYOUT_WIDTH_LIMIT = 600.dp

enum class LayoutWidth(val screenPadding: Dp) {
    COMPACT(16.dp),
    EXPANDED(24.dp);

    companion object {

        fun of(availableWidth: Dp): LayoutWidth =
            if (availableWidth < COMPACT_LAYOUT_WIDTH_LIMIT) COMPACT else EXPANDED
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksScreen.kt` — modified

Tasks reads the same value; its rendering is unchanged (all 20 tasks tests and `TasksHostTest` still pass).

_Change 1 of 1 (old line 37 / new line 37)_

**Old:**

```kotlin
            modifier = Modifier
                .fillMaxSize()
                .padding(if (layoutWidth == LayoutWidth.COMPACT) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
```

**New:**

```kotlin
            modifier = Modifier
                .fillMaxSize()
                .padding(layoutWidth.screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
```

### Step 3 — Social battery UI: build, ViewModel, DI

#### `shared/ui/social-battery/build.gradle.kts` — modified

Adds Metro, the core, `ui:common`, lifecycle and the JVM UI-test dependencies — the same set as `shared/ui/tasks/build.gradle.kts`.

**Old:**

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "ui-${project.name}"; isStatic = true } }
    jvm()
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    android {
       namespace = "org.orev.nahidka.ui.${project.name.replace("-", "")}"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
```

**New:**

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "ui-${project.name}"; isStatic = true } }
    jvm()
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    android {
       namespace = "org.orev.nahidka.ui.${project.name.replace("-", "")}"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:feature:social-battery"))
            implementation(project(":shared:ui:common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.metro.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.uiTestJunit4)
        }
    }
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryViewModel.kt` — new

Exposes the core `StateFlow` as is (no mapping, no second copy of the state) and forwards writes to `SocialBatteryManager`. Retained by the app session, so the level survives navigation.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

@Inject
class SocialBatteryViewModel(
    socialBatteryContext: SocialBatteryContext,
    private val socialBatteryManager: SocialBatteryManager,
) : ViewModel() {

    val batteryState: StateFlow<SocialBatterySnapshot> = socialBatteryContext.batteryState

    fun updateBattery(socialBattery: SocialBattery) {
        viewModelScope.launch {
            socialBatteryManager.updateBattery(socialBattery)
        }
    }
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryScreenGraph.kt` — new

DI composition root for the host and the tests; includes the core `SocialBatteryBindings`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.socialbattery.di.SocialBatteryBindings
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionScope

@DependencyGraph(SocialBatterySessionScope::class, bindingContainers = [SocialBatteryBindings::class])
interface SocialBatteryScreenGraph {
    val socialBatteryViewModel: SocialBatteryViewModel
}
```

### Step 4 — String resources (en / uk / ru)

The existing `feature_socialbattery_title` key is kept and becomes the battery's accessibility label. New keys follow the `<module>_*` convention of the other UI modules.

#### `shared/ui/social-battery/src/commonMain/composeResources/values/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_socialbattery_title">Social Battery</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_socialbattery_title">Social Battery</string>
    <string name="socialbattery_unset">How charged do you feel?</string>
    <string name="socialbattery_charge_low">Drained — time to recharge</string>
    <string name="socialbattery_charge_medium">Steady — something calm will do</string>
    <string name="socialbattery_charge_high">Charged — ready to socialize</string>
    <string name="socialbattery_hint">Tap or drag the battery to set your level</string>
</resources>
```

#### `shared/ui/social-battery/src/commonMain/composeResources/values-uk/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_socialbattery_title">Соціальна батарея</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_socialbattery_title">Соціальна батарея</string>
    <string name="socialbattery_unset">Наскільки ви заряджені?</string>
    <string name="socialbattery_charge_low">Розряджено — час перезарядитися</string>
    <string name="socialbattery_charge_medium">Стабільно — підійде щось спокійне</string>
    <string name="socialbattery_charge_high">Заряджено — час для спілкування</string>
    <string name="socialbattery_hint">Торкніться батареї або потягніть її, щоб указати рівень</string>
</resources>
```

#### `shared/ui/social-battery/src/commonMain/composeResources/values-ru/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_socialbattery_title">Социальная батарея</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_socialbattery_title">Социальная батарея</string>
    <string name="socialbattery_unset">Насколько вы заряжены?</string>
    <string name="socialbattery_charge_low">Разряжено — пора перезарядиться</string>
    <string name="socialbattery_charge_medium">Стабильно — подойдёт что-то спокойное</string>
    <string name="socialbattery_charge_high">Заряжено — время для общения</string>
    <string name="socialbattery_hint">Коснитесь батареи или потяните её, чтобы указать уровень</string>
</resources>
```

### Step 5 — Battery and particles (`battery/`)

Each file has one job: conversion, charge level, geometry, drawing + input, particle model, particle animation, sizing.

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryChargeFraction.kt` — new

The only place that converts between `SocialBattery` and the `0f..1f` fraction used for drawing, pointer input and accessibility. Clamping uses the core range.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import kotlin.math.roundToInt

private val FULL_CHARGE_PERCENTAGE = SocialBattery.PERCENTAGE_RANGE.last.toFloat()

internal val CHARGE_FRACTION_STEPS = SocialBattery.PERCENTAGE_RANGE.last - SocialBattery.PERCENTAGE_RANGE.first - 1

internal fun SocialBattery?.chargeFraction(): Float =
    (this?.percentage ?: SocialBattery.PERCENTAGE_RANGE.first) / FULL_CHARGE_PERCENTAGE

internal fun socialBatteryOf(chargeFraction: Float): SocialBattery =
    SocialBattery(
        (chargeFraction * FULL_CHARGE_PERCENTAGE)
            .roundToInt()
            .coerceIn(SocialBattery.PERCENTAGE_RANGE),
    )
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryCharge.kt` — new

Low / medium / high: threshold, caption and theme color in one table. The fill, glow, particles and caption all take their color from here.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_charge_high
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_charge_low
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_charge_medium
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

internal enum class BatteryCharge(val minimumPercentage: Int, val description: StringResource) {
    LOW(0, Res.string.socialbattery_charge_low),
    MEDIUM(30, Res.string.socialbattery_charge_medium),
    HIGH(70, Res.string.socialbattery_charge_high);

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            LOW -> MaterialTheme.colorScheme.error
            MEDIUM -> MaterialTheme.colorScheme.secondary
            HIGH -> MaterialTheme.colorScheme.primary
        }

    companion object {

        fun of(socialBattery: SocialBattery): BatteryCharge =
            entries.last { batteryCharge -> socialBattery.percentage >= batteryCharge.minimumPercentage }
    }
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryGeometry.kt` — new

Terminal, casing and cell rectangles computed from the battery size, used both for drawing and for turning a pointer position into a level — so what you see is exactly what you touch.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

internal const val BATTERY_ASPECT_RATIO = 0.54f

private const val TERMINAL_WIDTH_FRACTION = 0.26f
private const val TERMINAL_HEIGHT_FRACTION = 0.06f
private const val TERMINAL_CORNER_RADIUS_FRACTION = 0.03f
private const val CASING_STROKE_WIDTH_FRACTION = 0.035f
private const val CASING_CORNER_RADIUS_FRACTION = 0.12f
private const val CELL_GAP_FRACTION = 0.035f

internal class BatteryGeometry(batterySize: Size) {

    val casingStrokeWidth = batterySize.width * CASING_STROKE_WIDTH_FRACTION

    val terminal = Rect(
        left = batterySize.width * (1 - TERMINAL_WIDTH_FRACTION) / 2,
        top = 0f,
        right = batterySize.width * (1 + TERMINAL_WIDTH_FRACTION) / 2,
        bottom = batterySize.height * TERMINAL_HEIGHT_FRACTION + casingStrokeWidth,
    )

    val terminalCornerRadius = CornerRadius(batterySize.width * TERMINAL_CORNER_RADIUS_FRACTION)

    val casing = Rect(
        left = casingStrokeWidth / 2,
        top = batterySize.height * TERMINAL_HEIGHT_FRACTION + casingStrokeWidth / 2,
        right = batterySize.width - casingStrokeWidth / 2,
        bottom = batterySize.height - casingStrokeWidth / 2,
    )

    val casingCornerRadius = CornerRadius(batterySize.width * CASING_CORNER_RADIUS_FRACTION)

    val cell = casing.deflate(casingStrokeWidth / 2 + batterySize.width * CELL_GAP_FRACTION)

    val cellCornerRadius = CornerRadius(casingCornerRadius.x - (casing.left - cell.left))

    fun chargeArea(chargeFraction: Float): Rect =
        cell.copy(top = cell.bottom - cell.height * chargeFraction)

    fun chargeFractionAt(verticalPosition: Float): Float =
        ((cell.bottom - verticalPosition) / cell.height).coerceIn(0f, 1f)
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryGauge.kt` — new

Draws the battery and the percentage on one `Canvas`. One gesture loop handles tap and drag for touch and mouse. Semantics make it an adjustable slider for TalkBack / VoiceOver / desktop accessibility.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.toSize
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.feature_socialbattery_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

private const val UNSET_PERCENTAGE_LABEL = "—"
private const val PERCENTAGE_FONT_SIZE_FRACTION = 0.2f
private const val CHARGE_TOP_ALPHA = 0.3f
private const val CHARGE_BOTTOM_ALPHA = 0.75f

@Composable
internal fun BatteryGauge(
    socialBattery: SocialBattery?,
    chargeFraction: Float,
    chargeColor: Color,
    onBatteryChange: (SocialBattery) -> Unit,
    modifier: Modifier = Modifier,
) {
    val batteryTitle = stringResource(Res.string.feature_socialbattery_title)
    val percentageLabel = socialBattery?.let { chargedBattery -> "${chargedBattery.percentage}%" } ?: UNSET_PERCENTAGE_LABEL
    val textMeasurer = rememberTextMeasurer()
    val percentageTextStyle = MaterialTheme.typography.displayMedium
    val casingColor = MaterialTheme.colorScheme.onSurface
    val cellColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = batteryTitle
                stateDescription = percentageLabel
                progressBarRangeInfo = ProgressBarRangeInfo(socialBattery.chargeFraction(), 0f..1f, CHARGE_FRACTION_STEPS)
                setProgress { targetChargeFraction ->
                    onBatteryChange(socialBatteryOf(targetChargeFraction))
                    true
                }
            }
            .pointerInput(onBatteryChange) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown()
                    onBatteryChange(socialBatteryAt(firstDown.position))
                    verticalDrag(firstDown.id) { pointerChange ->
                        pointerChange.consume()
                        onBatteryChange(socialBatteryAt(pointerChange.position))
                    }
                }
            },
    ) {
        val batteryGeometry = BatteryGeometry(size)
        val chargeArea = batteryGeometry.chargeArea(chargeFraction)
        val percentageTextLayout = textMeasurer.measure(
            text = percentageLabel,
            style = percentageTextStyle.copy(
                color = casingColor,
                fontSize = (size.width * PERCENTAGE_FONT_SIZE_FRACTION).toSp(),
            ),
        )

        drawRoundRect(
            color = casingColor,
            topLeft = batteryGeometry.terminal.topLeft,
            size = batteryGeometry.terminal.size,
            cornerRadius = batteryGeometry.terminalCornerRadius,
        )
        drawRoundRect(
            color = cellColor,
            topLeft = batteryGeometry.casing.topLeft,
            size = batteryGeometry.casing.size,
            cornerRadius = batteryGeometry.casingCornerRadius,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(chargeColor.copy(alpha = CHARGE_TOP_ALPHA), chargeColor.copy(alpha = CHARGE_BOTTOM_ALPHA)),
                startY = chargeArea.top,
                endY = chargeArea.bottom,
            ),
            topLeft = chargeArea.topLeft,
            size = chargeArea.size,
            cornerRadius = batteryGeometry.cellCornerRadius,
        )
        drawRoundRect(
            color = casingColor,
            topLeft = batteryGeometry.casing.topLeft,
            size = batteryGeometry.casing.size,
            cornerRadius = batteryGeometry.casingCornerRadius,
            style = Stroke(batteryGeometry.casingStrokeWidth),
        )
        drawText(
            textLayoutResult = percentageTextLayout,
            topLeft = batteryGeometry.cell.center - percentageTextLayout.size.toSize().center,
        )
    }
}

private fun PointerInputScope.socialBatteryAt(pointerPosition: Offset): SocialBattery =
    socialBatteryOf(
        BatteryGeometry(size.toSize()).chargeFractionAt(pointerPosition.y),
    )
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryParticle.kt` — new

One particle: direction, phase, speed (whole loops per animation cycle, so the loop is seamless), size, and the level above which it is visible.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.ui.unit.Dp

internal class BatteryParticle(
    val direction: Float,
    val travelOffset: Float,
    val travelCycles: Int,
    val radius: Dp,
    val chargeThreshold: Float,
) {

    fun travelProgress(animationProgress: Float): Float =
        (animationProgress * travelCycles + travelOffset) % 1f
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryParticles.kt` — new

96 seeded particles drift from the battery edge to the field edge and fade, over a radial glow. The animation clock is read only in the draw phase, so frames redraw without recomposing.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PARTICLE_COUNT = 96
private const val PARTICLE_RANDOM_SEED = 2026
private const val PARTICLE_ANIMATION_DURATION_MILLISECONDS = 18_000
private const val MAXIMUM_PARTICLE_TRAVEL_CYCLES = 4
private const val FULL_TURN = 2 * PI.toFloat()
private const val GLOW_ALPHA = 0.18f
private val MINIMUM_PARTICLE_RADIUS = 1.5.dp
private val PARTICLE_RADIUS_SPREAD = 2.5.dp

private val BATTERY_PARTICLES = Random(PARTICLE_RANDOM_SEED).let { random ->
    List(PARTICLE_COUNT) { particleIndex ->
        BatteryParticle(
            direction = random.nextFloat() * FULL_TURN,
            travelOffset = random.nextFloat(),
            travelCycles = random.nextInt(1, MAXIMUM_PARTICLE_TRAVEL_CYCLES + 1),
            radius = MINIMUM_PARTICLE_RADIUS + PARTICLE_RADIUS_SPREAD * random.nextFloat(),
            chargeThreshold = particleIndex.toFloat() / PARTICLE_COUNT,
        )
    }
}

@Composable
internal fun BatteryParticles(
    chargeFraction: Float,
    chargeColor: Color,
    batterySize: DpSize,
    modifier: Modifier = Modifier,
) {
    val animationProgress by rememberInfiniteTransition()
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(PARTICLE_ANIMATION_DURATION_MILLISECONDS, easing = LinearEasing)),
        )

    Canvas(modifier) {
        val emissionRadius = Offset(batterySize.width.toPx() / 2, batterySize.height.toPx() / 2)
        val travelDistance = center - emissionRadius

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(chargeColor.copy(alpha = GLOW_ALPHA * chargeFraction), Color.Transparent),
                center = center,
                radius = size.maxDimension / 2,
            ),
            radius = size.maxDimension / 2,
        )
        BATTERY_PARTICLES
            .filter { batteryParticle -> batteryParticle.chargeThreshold < chargeFraction }
            .forEach { batteryParticle ->
                val travelProgress = batteryParticle.travelProgress(animationProgress)

                drawCircle(
                    color = chargeColor,
                    radius = batteryParticle.radius.toPx(),
                    center = center + Offset(
                        x = cos(batteryParticle.direction) * (emissionRadius.x + travelDistance.x * travelProgress),
                        y = sin(batteryParticle.direction) * (emissionRadius.y + travelDistance.y * travelProgress),
                    ),
                    alpha = 1f - travelProgress,
                )
            }
    }
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryField.kt` — new

Sizes the battery from the available width *and* height and puts the particle field behind it. The level animates smoothly between values.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

private const val BATTERY_MAXIMUM_WIDTH_FRACTION = 0.6f
private const val BATTERY_MAXIMUM_HEIGHT_FRACTION = 0.74f
private const val PARTICLE_FIELD_WIDTH_SCALE = 2.4f
private const val PARTICLE_FIELD_HEIGHT_SCALE = 1.35f

@Composable
internal fun BatteryField(
    socialBattery: SocialBattery?,
    chargeColor: Color,
    onBatteryChange: (SocialBattery) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chargeFraction by animateFloatAsState(socialBattery.chargeFraction())

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val batteryHeight = minOf(
            maxHeight * BATTERY_MAXIMUM_HEIGHT_FRACTION,
            maxWidth * BATTERY_MAXIMUM_WIDTH_FRACTION / BATTERY_ASPECT_RATIO,
        )
        val batterySize = DpSize(batteryHeight * BATTERY_ASPECT_RATIO, batteryHeight)

        BatteryParticles(
            chargeFraction = chargeFraction,
            chargeColor = chargeColor,
            batterySize = batterySize,
            modifier = Modifier.size(batterySize.width * PARTICLE_FIELD_WIDTH_SCALE, batterySize.height * PARTICLE_FIELD_HEIGHT_SCALE),
        )
        BatteryGauge(
            socialBattery = socialBattery,
            chargeFraction = chargeFraction,
            chargeColor = chargeColor,
            onBatteryChange = onBatteryChange,
            modifier = Modifier.size(batterySize),
        )
    }
}
```

### Step 6 — Screen entry point

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryScreen.kt` — new

Public entry point: shared padding rule, battery field, charge caption and hint, centered as one group.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_hint
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_unset
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.socialbattery.battery.BatteryCharge
import org.orev.nahidka.ui.socialbattery.battery.BatteryField

@Composable
fun SocialBatteryScreen(socialBatteryViewModel: SocialBatteryViewModel, modifier: Modifier = Modifier) {
    val batterySnapshot by socialBatteryViewModel.batteryState.collectAsStateWithLifecycle()
    val socialBattery = batterySnapshot.socialBattery
    val batteryCharge = socialBattery?.let(BatteryCharge::of)
    val chargeColor = batteryCharge?.color ?: MaterialTheme.colorScheme.outline

    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LayoutWidth.of(maxWidth).screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            BatteryField(
                socialBattery = socialBattery,
                chargeColor = chargeColor,
                onBatteryChange = socialBatteryViewModel::updateBattery,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            )
            Text(
                text = stringResource(batteryCharge?.description ?: Res.string.socialbattery_unset),
                color = chargeColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.socialbattery_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
```

#### `shared/ui/social-battery/src/commonMain/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryWidget.kt` — deleted

Its only caller, the slider tab in `PersonalFeaturesScreen`, is replaced by `SocialBatteryScreen` (Step 8). The dashboard declares the module dependency but never used the widget.

**Old:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SocialBatteryWidget(batteryLevel: Float, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Social Battery", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { batteryLevel },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
        )
        Text("${(batteryLevel * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
}
```

**New:** _(file deleted — nothing replaces it at this path; `SocialBatteryScreen` takes over its role)_

#### `shared/ui/social-battery/README.md` — new

Module overview and verification commands, like `shared/ui/tasks/README.md`.

**Old:** _(file does not exist)_

**New:**

````markdown
# Social battery UI

`SocialBatteryScreen` shows the user's social battery as a large battery surrounded by animated particles. Tap or drag inside the battery to set the level; screen readers adjust it like a slider. The fill, glow, particles, and caption follow the charge: low (0–29 %), medium (30–69 %), and high (70–100 %). An unset battery shows `—` and asks for a level. Strings are available in English, Ukrainian, and Russian.

The layout measures its own width with the shared 600 dp breakpoint (`LayoutWidth`) and sizes the battery from the available width and height, so phones in portrait and landscape, tablets, desktop windows, and browsers all fit without scrolling.

`SocialBatteryScreenGraph` uses the same `SocialBatteryBindings` as the core `SocialBatterySessionGraph`. The app retains the ViewModel in its session and exposes a Social battery entry. The level stays in memory for the app session.

## Verification

```sh
./gradlew :shared:core:feature:social-battery:jvmTest :shared:ui:common:jvmTest :shared:ui:social-battery:jvmTest :shared:ui:tasks:jvmTest --rerun
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
```

`SocialBatteryLayoutTest` renders desktop light and dark screens and Ukrainian phone screens in portrait and landscape. PNG previews are written to `/tmp/nahidka-social-battery-previews`.
````

### Step 7 — Tests (`src/jvmTest`)

UI tests drive the real `SocialBatteryScreen` built from the real `SocialBatteryScreenGraph`, with real touch input and the real accessibility action.

#### `shared/ui/social-battery/src/jvmTest/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryTest.kt` — new

Shared base: Main dispatcher and the ViewModel from the production graph.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class SocialBatteryTest {

    protected val socialBatteryViewModel by lazy { createGraph<SocialBatteryScreenGraph>().socialBatteryViewModel }

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }
}
```

#### `shared/ui/social-battery/src/jvmTest/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryViewModelTest.kt` — new

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class SocialBatteryViewModelTest : SocialBatteryTest() {

    @Test
    fun batteryStartsUnset() {
        assertEquals(SocialBatterySnapshot(0, null), socialBatteryViewModel.batteryState.value)
    }

    @Test
    fun updatingBatteryPublishesOneRevisionPerChange() {
        socialBatteryViewModel.updateBattery(SocialBattery(80))
        socialBatteryViewModel.updateBattery(SocialBattery(80))
        socialBatteryViewModel.updateBattery(SocialBattery(0))

        assertEquals(SocialBatterySnapshot(2, SocialBattery(0)), socialBatteryViewModel.batteryState.value)
    }
}
```

#### `shared/ui/social-battery/src/jvmTest/kotlin/org/orev/nahidka/ui/socialbattery/battery/BatteryChargeTest.kt` — new

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery.battery

import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryChargeTest {

    @Test
    fun chargeFractionConvertsBothWays() {
        assertEquals(0f, null.chargeFraction())
        assertEquals(0.45f, SocialBattery(45).chargeFraction())
        assertEquals(SocialBattery(90), socialBatteryOf(0.904f))
        assertEquals(SocialBattery(0), socialBatteryOf(-0.2f))
        assertEquals(SocialBattery(100), socialBatteryOf(1.3f))
    }

    @Test
    fun chargeLevelsStartAtTheirMinimumPercentage() {
        assertEquals(
            listOf(BatteryCharge.LOW, BatteryCharge.LOW, BatteryCharge.MEDIUM, BatteryCharge.MEDIUM, BatteryCharge.HIGH, BatteryCharge.HIGH),
            listOf(0, 29, 30, 69, 70, 100).map { percentage -> BatteryCharge.of(SocialBattery(percentage)) },
        )
    }
}
```

#### `shared/ui/social-battery/src/jvmTest/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryScreenTest.kt` — new

Unset state, tap, drag, and the screen-reader action (exact value, range info and spoken state).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BATTERY_TITLE = "Social Battery"

@OptIn(ExperimentalTestApi::class)
class SocialBatteryScreenTest : SocialBatteryTest() {

    @Test
    fun unsetBatteryAsksForLevel() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        onNodeWithContentDescription(BATTERY_TITLE)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "—"))
    }

    @Test
    fun tappingBatteryTopChargesItFully() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithContentDescription(BATTERY_TITLE)
            .performTouchInput { click(topCenter) }

        onNodeWithText("Charged — ready to socialize")
            .assertIsDisplayed()
        assertEquals(SocialBattery(100), socialBatteryViewModel.batteryState.value.socialBattery)
    }

    @Test
    fun draggingBatteryDownDrainsIt() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithContentDescription(BATTERY_TITLE)
            .performTouchInput {
                down(topCenter)
                moveTo(center)
                moveTo(bottomCenter)
                up()
            }

        onNodeWithText("Drained — time to recharge")
            .assertIsDisplayed()
        assertEquals(SocialBattery(0), socialBatteryViewModel.batteryState.value.socialBattery)
    }

    @Test
    fun accessibilityActionSetsExactLevel() = runComposeUiTest {
        showSocialBatteryScreen(width = 360.dp)

        onNodeWithContentDescription(BATTERY_TITLE)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.42f) }

        onNodeWithText("Steady — something calm will do")
            .assertIsDisplayed()
        onNodeWithContentDescription(BATTERY_TITLE)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.42f, 0f..1f, 99))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "42%"))
    }

    private fun ComposeUiTest.showSocialBatteryScreen(width: Dp = 1200.dp) {
        setContent {
            SocialBatteryScreen(
                socialBatteryViewModel,
                Modifier.size(width, 800.dp),
            )
        }
    }
}
```

#### `shared/ui/social-battery/src/jvmTest/kotlin/org/orev/nahidka/ui/socialbattery/SocialBatteryLayoutTest.kt` — new

Renders every charge level in light and dark at 1200×800 and Ukrainian phones at 360×800 and 800×360, asserting the captions fit; PNGs go to `/tmp/nahidka-social-battery-previews` (the same pattern as `TasksLayoutTest`).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

private const val PREVIEW_TAG = "social-battery-preview"
private const val CHARGE_ANIMATION_MILLISECONDS = 2_000L

@OptIn(ExperimentalTestApi::class)
class SocialBatteryLayoutTest : SocialBatteryTest() {

    @Test
    fun expandedLayoutRendersEveryChargeInBothThemes() = runSkikoComposeUiTest(size = Size(1200f, 800f)) {
        val darkTheme = mutableStateOf(false)
        setContent {
            NahidkaTheme(darkTheme.value) {
                Surface {
                    SocialBatteryScreen(
                        socialBatteryViewModel,
                        Modifier
                            .size(1200.dp, 800.dp)
                            .testTag(PREVIEW_TAG),
                    )
                }
            }
        }
        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        saveScreen("desktop-unset-light")
        showCharge(90, "Charged — ready to socialize")
        saveScreen("desktop-high-light")
        darkTheme.value = true
        saveScreen("desktop-high-dark")
        showCharge(45, "Steady — something calm will do")
        saveScreen("desktop-medium-dark")
        showCharge(12, "Drained — time to recharge")
        saveScreen("desktop-low-dark")
    }

    @Test
    fun ukrainianCompactLayoutsFitPortraitAndLandscapePhones() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("uk"))
        try {
            renderPhone(width = 360, height = 800, name = "phone-portrait-uk")
            renderPhone(width = 800, height = 360, name = "phone-landscape-uk")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun renderPhone(width: Int, height: Int, name: String) =
        runSkikoComposeUiTest(size = Size(width.toFloat(), height.toFloat())) {
            socialBatteryViewModel.updateBattery(SocialBattery(100))
            setContent {
                NahidkaTheme(darkTheme = true) {
                    Surface {
                        SocialBatteryScreen(
                            socialBatteryViewModel,
                            Modifier
                                .size(width.dp, height.dp)
                                .testTag(PREVIEW_TAG),
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(CHARGE_ANIMATION_MILLISECONDS)
            onNodeWithText("Заряджено — час для спілкування")
                .assertIsDisplayed()
            onNodeWithText("Торкніться батареї або потягніть її, щоб указати рівень")
                .assertIsDisplayed()
            saveScreen(name)
        }

    private fun ComposeUiTest.showCharge(percentage: Int, description: String) {
        socialBatteryViewModel.updateBattery(SocialBattery(percentage))
        mainClock.advanceTimeBy(CHARGE_ANIMATION_MILLISECONDS)
        onNodeWithText(description)
            .assertIsDisplayed()
    }

    private fun ComposeUiTest.saveScreen(name: String) {
        val bitmap = onNodeWithTag(PREVIEW_TAG).captureToImage()
        val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
        val encodedImage = checkNotNull(image.encodeToData())
        val outputDirectory = File("/tmp/nahidka-social-battery-previews")
        assertTrue(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name.png").writeBytes(encodedImage.bytes)
        encodedImage.close()
        image.close()
    }
}
```

### Step 8 — Host integration (`shared/`)

Replaces the hand-built `SocialBatteryContext` / `SocialBatteryManager` and the slider tab with the graph, a session-retained ViewModel and `SocialBatteryScreen` — the same wiring as Tasks. The side menu itself stays out of scope.

#### `shared/src/commonMain/kotlin/org/orev/nahidka/App.kt` — modified

Adds the “Social battery” top-bar entry. Overview and the personal-feature entries reset `showingSocialBattery`; Settings and Tasks don't need to, because they come earlier in the `if` chain — the precedence the existing flags already rely on. `SocialBatteryHostTest` covers leaving and returning.

_Change 1 of 7 (old line 33 / new line 33)_

**Old:**

```kotlin
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsManager
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.common.theme.NahidkaTheme
```

**New:**

```kotlin
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsManager
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.common.theme.NahidkaTheme
```

_Change 2 of 7 (old line 44 / new line 41)_

**Old:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.tasks.TasksScreen
import org.orev.nahidka.ui.tasks.TasksScreenGraph
```

**New:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.socialbattery.SocialBatteryScreen
import org.orev.nahidka.ui.socialbattery.SocialBatteryScreenGraph
import org.orev.nahidka.ui.socialbattery.SocialBatteryViewModel
import org.orev.nahidka.ui.tasks.TasksScreen
import org.orev.nahidka.ui.tasks.TasksScreenGraph
```

_Change 3 of 7 (old line 80 / new line 80)_

**Old:**

```kotlin
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
    val batteryContext = remember(session) { SocialBatteryContext() }
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val goalsSnapshot by goalsContext.goalsState.collectAsState()
    val batterySnapshot by batteryContext.batteryState.collectAsState()
    var selectedPersonalFeature by remember { mutableStateOf<PersonalFeature?>(null) }
    var personalFeatureError by remember { mutableStateOf<String?>(null) }
```

**New:**

```kotlin
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
    val socialBatteryScreenGraph = remember(session) { createGraph<SocialBatteryScreenGraph>() }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val goalsSnapshot by goalsContext.goalsState.collectAsState()
    var selectedPersonalFeature by remember { mutableStateOf<PersonalFeature?>(null) }
    var personalFeatureError by remember { mutableStateOf<String?>(null) }
```

_Change 4 of 7 (old line 103 / new line 101)_

**Old:**

```kotlin
    }
    var showingTasks by remember { mutableStateOf(false) }

    val dashboardViewModel = viewModel<DashboardViewModel>(
```

**New:**

```kotlin
    }
    var showingTasks by remember { mutableStateOf(false) }
    var showingSocialBattery by remember { mutableStateOf(false) }

    val dashboardViewModel = viewModel<DashboardViewModel>(
```

_Change 5 of 7 (old line 118 / new line 117)_

**Old:**

```kotlin
    }

    val financialHistoryViewModel = viewModel<FinancialHistoryViewModel>(
        viewModelStoreOwner = session,
```

**New:**

```kotlin
    }

    val socialBatteryViewModel = viewModel<SocialBatteryViewModel>(
        viewModelStoreOwner = session,
        key = "social-battery",
    ) {
        socialBatteryScreenGraph.socialBatteryViewModel
    }

    val financialHistoryViewModel = viewModel<FinancialHistoryViewModel>(
        viewModelStoreOwner = session,
```

_Change 6 of 7 (old line 137 / new line 143)_

**Old:**

```kotlin
            titleBar?.invoke()
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { showingSettings = false; showingFinance = false; showingTasks = false; selectedPersonalFeature = null }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true; selectedPersonalFeature = null }) { Text("Settings") }
                TextButton(onClick = { showingSettings = false; showingTasks = true; selectedPersonalFeature = null }) { Text("Tasks") }
                PersonalFeature.entries.forEach { feature ->
                    TextButton(onClick = { showingSettings = false; showingTasks = false; selectedPersonalFeature = feature }) {
                        Text(feature.name.lowercase().replace('_', ' '))
                    }
```

**New:**

```kotlin
            titleBar?.invoke()
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { showingSettings = false; showingFinance = false; showingTasks = false; showingSocialBattery = false; selectedPersonalFeature = null }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true; selectedPersonalFeature = null }) { Text("Settings") }
                TextButton(onClick = { showingSettings = false; showingTasks = true; selectedPersonalFeature = null }) { Text("Tasks") }
                TextButton(onClick = { showingSettings = false; showingTasks = false; showingSocialBattery = true; selectedPersonalFeature = null }) { Text("Social battery") }
                PersonalFeature.entries.forEach { feature ->
                    TextButton(onClick = { showingSettings = false; showingTasks = false; showingSocialBattery = false; selectedPersonalFeature = feature }) {
                        Text(feature.name.lowercase().replace('_', ' '))
                    }
```

_Change 7 of 7 (old line 162 / new line 169)_

**Old:**

```kotlin
                } else if (showingTasks) {
                    TasksScreen(tasksViewModel)
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        goals = goalsSnapshot.goals,
                        batteryPercentage = batterySnapshot.socialBattery?.percentage,
                        onCreateGoal = { title -> mutatePersonalFeature { goalsManager.createGoal(GoalCreationRequest(identifierGenerator.next(), title)) } },
                        onUpdateGoal = { goal -> mutatePersonalFeature {
                            goalsManager.updateGoal(GoalUpdateRequest(goal.identifier, progressPercentage = (goal.progressPercentage + 10f).coerceAtMost(100f)))
                        } },
                        onUpdateBattery = { percentage -> mutatePersonalFeature { batteryManager.updateBattery(SocialBattery(percentage)) } },
                        errorMessage = personalFeatureError,
                    )
```

**New:**

```kotlin
                } else if (showingTasks) {
                    TasksScreen(tasksViewModel)
                } else if (showingSocialBattery) {
                    SocialBatteryScreen(socialBatteryViewModel)
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        goals = goalsSnapshot.goals,
                        onCreateGoal = { title -> mutatePersonalFeature { goalsManager.createGoal(GoalCreationRequest(identifierGenerator.next(), title)) } },
                        onUpdateGoal = { goal -> mutatePersonalFeature {
                            goalsManager.updateGoal(GoalUpdateRequest(goal.identifier, progressPercentage = (goal.progressPercentage + 10f).coerceAtMost(100f)))
                        } },
                        errorMessage = personalFeatureError,
                    )
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/PersonalFeature.kt` — modified

`SOCIAL_BATTERY` leaves, as `TASKS` did.

**Old:**

```kotlin
package org.orev.nahidka

internal enum class PersonalFeature { GOALS, SOCIAL_BATTERY }
```

**New:**

```kotlin
package org.orev.nahidka

internal enum class PersonalFeature { GOALS }
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/PersonalFeaturesScreen.kt` — modified

Only the battery parameters and branch are removed; the goals code is unchanged apart from losing one indentation level.

**Old:**

```kotlin
package org.orev.nahidka

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.*
import org.orev.nahidka.ui.goal.GoalsTable
import org.orev.nahidka.ui.socialbattery.SocialBatteryWidget

@Composable
internal fun PersonalFeaturesScreen(
    feature: PersonalFeature,
    goals: List<GoalRecord>,
    batteryPercentage: Int?,
    onCreateGoal: (String) -> Unit,
    onUpdateGoal: (GoalRecord) -> Unit,
    onUpdateBattery: (Int) -> Unit,
    errorMessage: String?,
) {
    var newTitle by remember(feature) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (feature != PersonalFeature.SOCIAL_BATTERY) {
            Text("Goals", style = MaterialTheme.typography.headlineMedium)
            Row {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f))
                TextButton(enabled = newTitle.isNotBlank(), onClick = {
                    onCreateGoal(newTitle.trim())
                    newTitle = ""
                }) { Text("Add") }
            }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
            PersonalFeature.GOALS -> {
                Text("Select a goal to advance its progress by 10%.")
                GoalsTable(goals, onGoalClick = onUpdateGoal)
            }
            PersonalFeature.SOCIAL_BATTERY -> {
                if (batteryPercentage == null) Text("Choose your current social battery level.")
                else SocialBatteryWidget(batteryPercentage / 100f)
                Slider(
                    value = (batteryPercentage ?: 50).toFloat(),
                    onValueChange = { onUpdateBattery(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 99,
                )
            }
        }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.*
import org.orev.nahidka.ui.goal.GoalsTable

@Composable
internal fun PersonalFeaturesScreen(
    feature: PersonalFeature,
    goals: List<GoalRecord>,
    onCreateGoal: (String) -> Unit,
    onUpdateGoal: (GoalRecord) -> Unit,
    errorMessage: String?,
) {
    var newTitle by remember(feature) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Goals", style = MaterialTheme.typography.headlineMedium)
        Row {
            OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f))
            TextButton(enabled = newTitle.isNotBlank(), onClick = {
                onCreateGoal(newTitle.trim())
                newTitle = ""
            }) { Text("Add") }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
            PersonalFeature.GOALS -> {
                Text("Select a goal to advance its progress by 10%.")
                GoalsTable(goals, onGoalClick = onUpdateGoal)
            }
        }
    }
}
```

#### `shared/src/jvmTest/kotlin/org/orev/nahidka/SocialBatteryHostTest.kt` — new

Opens the real app, sets the level, leaves and comes back: the level and caption are preserved.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class SocialBatteryHostTest {

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun socialBatteryEntryPreservesLevelAcrossNavigation() = runComposeUiTest {
        setContent { App() }
        onNodeWithText("Social battery")
            .performClick()
        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        onNodeWithContentDescription("Social Battery")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.8f) }
        onNodeWithText("Overview")
            .performClick()
        onNodeWithText("Social battery")
            .performClick()
        onNodeWithContentDescription("Social Battery")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "80%"))
        onNodeWithText("Charged — ready to socialize")
            .assertIsDisplayed()
    }
}
```

## 5. Verification, limitations, out of scope

**Verified (in an isolated copy of the repository at `6df8e9d`):**
* **36 / 36 tests pass, green on 3 consecutive forced re-runs.** The 12 new ones are 1 core (`SocialBatteryTest`), 2 `SocialBatteryViewModelTest`, 2 `BatteryChargeTest`, 4 `SocialBatteryScreenTest`, 2 `SocialBatteryLayoutTest` and 1 `SocialBatteryHostTest`. The 24 existing ones were re-run because Step 2 touches shared code: 3 `DialogControllerTest`, 20 tasks tests and `TasksHostTest`.
* The UI tests use real input: a touch tap on the battery, a touch drag from top to bottom, and the screen-reader `SetProgress` action. **Negative check:** with the drag and accessibility handlers disabled, the drag and accessibility tests fail. The source was restored afterwards.
* `:shared:core:feature:social-battery`, `:shared:ui:common` and `:shared:ui:social-battery` compile for **JVM, JS, Wasm and Android**, main and test sources. `:androidApp:assembleDebug`, `:desktopApp:compileKotlin`, `:webApp:compileKotlinJs`, `:webApp:compileKotlinWasmJs` and `:shared:jvmTest` all succeed.
* No new compiler warnings, and the Metro warning on `SocialBatteryManager` is gone.
* The previews above were rendered from this code and inspected: desktop light and dark for every charge level and the unset state, and Ukrainian phones in portrait and landscape. The GIF came from a temporary test (not part of the plan) that drove the same renderer with a controlled clock.

**Not verified:**
* **iOS:** it can't be built on this Linux host.
* **A physical touchscreen:** touch was tested with Compose's synthetic touch input.

**Known limitations:**
* The level stays in memory for the app session, like the core today. Persistence and the backend are not part of this plan.
* There are no arrow-key controls on desktop or web. Screen readers can adjust the level, and the mouse works.
* The particle animation ignores the system *reduce motion* setting (decision 4).

**Out of scope, possible follow-ups:**
* **The side menu** and the app shell.
* **The dashboard's own battery.** `DashboardState.socialBatteryLevel` (`.87f`, changed by `DashboardEvent.SetBattery`) is separate sample state, not this core, so the dashboard and this screen can disagree. Connecting them means giving `DashboardViewModel` (created in `FinancialSessionGraph`) the same `SocialBatteryContext` instance as `SocialBatteryScreenGraph`. *I can plan that next if you want.*
* **A partner's battery.** The core holds one battery; the dashboard's "Partner: 85%" is hard-coded.
* **`FinancialTabLayout` padding.** It moves to `LayoutWidth.screenPadding` together with the rest of the financial migration to `ui:common`.

**Suggested implementation order:** Steps 1 → 8. The core compiles and its test passes after Step 1, Tasks still passes after Step 2, the UI module compiles after Steps 3–6, its tests run after Step 7, and the app runs after Step 8. You can delete `plan-previews/` once you've reviewed the plan; nothing references it except this file.
