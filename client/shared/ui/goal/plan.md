# Implementation plan — `shared/ui/goal`

**Status (2026-10-07): implemented and verified.** All ten steps are applied. The host also incorporates the concurrent social-battery implementation as described in §5. The 52 tests specified here pass on three consecutive forced runs; the merged host's additional social-battery test also passes. Android, desktop, JS, Wasm and both iOS klib targets build successfully. Current-worktree evidence, the requirement audit and runtime limitations are recorded in [verification.md](verification.md). The implementation blocks below and the original isolated-copy verification in §5 are retained as the specification.

---

## 1. What gets built (mockup → implementation)

| Mockup element | Implementation |
|---|---|
| `Create goal` | Right-aligned button that opens the goal editor: title, description, picture, progress, optional deadline |
| `ICON or PHOTO` | A square picture box: one of 12 **emoji**, or a **photo** from the device, picked with each platform's own picker (Android Photo Picker, desktop file dialog, browser file input, iOS photo picker). A goal without a picture shows its title's first letter |
| `Title of goal` | Title, up to 2 lines |
| `Description of goal` | Description, up to 3 lines, then `…` |
| `Compleated on 100%` | “**Completed on 75%**” plus a progress bar. At 100 % the text turns the primary color |
| *(not drawn)* Changing progress | In the editor: a **0–100 % slider in 5 % steps** with a live “Progress: 75%” label. Clicking a card opens the editor |
| *(not drawn)* Deadline | Optional date in the editor; `📅 18.04.2027` next to the progress bar |
| *(not drawn)* Card actions | ⋮ → **Edit** / **Delete** (with confirmation) |
| SIDE MENU | App shell, outside this module (unchanged). The temporary host top bar gets a **Goals** entry (Step 10), as Tasks did |

### Mobile adaptivity

The screen measures **its own width** (`BoxWithConstraints`), so a narrow desktop window or browser behaves like a phone. The 600 dp breakpoint and the padding come from `LayoutWidth` in `shared/ui/common`, shared with Tasks.

| | Expanded (≥ 600 dp: desktop, web, tablets) | Compact (< 600 dp: phones) |
|---|---|---|
| Header | `[Create goal]` button, right-aligned | `[＋]` icon button, right-aligned (screen readers hear “Create goal”) |
| Picture | 112 dp square | 64 dp square |
| “Completed on N%” | Right of the title, next to ⋮ | Its own line under the title |
| Description | Up to 3 lines | Up to 3 lines over the narrower width |
| Progress bar and deadline | One row; the bar fills the remaining width | Same |
| Editor | Material dialog; the content scrolls | Same; the emoji chips wrap to three rows, and long Ukrainian labels wrap |
| Padding | 24 dp | 16 dp |

Checked at 1200 × 800 (light and dark) and at 360 × 800 in Ukrainian, which has the longest labels. No control was clipped.

---

## 2. Decisions

**Confirmed in chat**

1. **Emoji or photo, with our own picker.** The picker lives in `shared/ui/common` (`rememberPhotoPicker`), with one implementation per platform and **no third-party dependency**. Emoji are chosen from 12 chips in the editor. **Choose photo** opens the system picker, and **Remove picture** goes back to the letter.
2. **Progress is a slider in the editor**, 0–100 % in 5 % steps. Cards show the percentage and a progress bar.

**Decisions I made — please confirm or redirect**

1. **Core additions (allowed by your brief).** `GoalRecord` gains `description` and `picture` (`GoalPicture.Emoji` / `GoalPicture.Photo`). The deadline becomes date-only: `deadlineInstant: Instant?` → `deadlineDate: LocalDate?`. That lets Goals reuse the date field, `DAY_FORMAT` and the date text that Tasks already uses, and it avoids time-zone conversion. The project isn't released, and nothing else reads the field. `GoalRecord.PROGRESS_PERCENTAGE_RANGE` becomes the single source of `0..100`.
2. **Photos are stored as the original bytes**, in memory, like all goal data today. Two photos are equal when their bytes are equal, so re-saving the same photo isn't a change. Cards decode off the main thread and scale to ≤ 512 px to keep memory small. Shrinking the *stored* bytes needs an image encoder for each platform, so it waits for persistence and the backend (§5).
3. **Goals stay in creation order** (the core snapshot order). There is no sorting or filtering, since neither is in the mockup.
4. **The deadline is optional**, with no overdue highlight, as in Tasks.
5. **DRY across Tasks and Goals.** Every piece that Goals would otherwise copy from Tasks moves to `shared/ui/common` (Step 2), and Tasks switches to it (Step 3). Its tests pass unchanged. The DRY map in §3 lists each piece and its users.
6. **`ui/common` strings become public** (`publicResClass = true`). Save, Edit, Delete, Title, Description and “This change could not be saved” each exist once instead of once per feature. A file that needs both its feature's strings and the shared ones imports the shared class as `CommonResources`.
7. **Deletion texts stay per feature.** Ukrainian and Russian word forms differ by noun (“Завдання … буде видалено” / “Ціль … буде видалено”), so only the dialog shape is shared (`DeletionDialog`).
8. **Errors.** The core rejects invalid input with `IllegalArgumentException`. Dialogs show “This change could not be saved” and stay open. Every change goes through a dialog, so Goals needs no snackbar.
9. **DI.** `GoalsBindings` is shared by `GoalsSessionGraph` and `GoalsScreenGraph`, as `TasksBindings` is. `IdentifierGeneratorBindings` replaces the provider copies in `TasksScreenGraph` and `FinancialSessionGraph`. `GoalsManager` gets a class-level `@Inject`, which removes its existing Metro warning.
10. **Host:** goals get their own top-bar entry, `PersonalFeature` loses `GOALS`, and the old `GoalsTable` (tap to add 10 %) is deleted.
11. **Strings:** en / uk / ru, all translated.
12. **This overlaps with the pending social-battery plan** (`shared/ui/social-battery/plan.md`, not applied yet). Both add the same `LayoutWidth.screenPadding`, and both edit the host. §5 says how to apply both.

**How your style rules are applied:** there are no comments in any source file, tests included. Names are full words, including every lambda parameter (no implicit `it`); `_` appears only for a parameter the code ignores. Builder, `Modifier`, flow and test-finder chains put one call per line. `@Inject` is class-level, and each file holds one class or one composable family. Imports follow the IntelliJ layout: wildcards from 5 names, `kotlin.*` last, and alias imports at the end. The new code adds no compiler warnings on any target and removes one existing warning.

---

## 3. Architecture

```
shared/core/feature/common                 (Step 1)
└── IdentifierGeneratorBindings            one identifier provider for every graph

shared/core/feature/goals                  (Step 1)
├── dto/      GoalRecord(+description, +picture, deadlineDate, PROGRESS_PERCENTAGE_RANGE)
│             GoalPicture (Emoji | Photo) · GoalCreationRequest · GoalUpdateRequest(+picturePatch)
├── service/  GoalsContext (state + all validation) · GoalsManager (writes)
└── di/       GoalsBindings (shared providers) · GoalsSessionGraph

shared/ui/common                           (Step 2, feature-agnostic)
├── layout/    LayoutWidth(+screenPadding)
├── component/ TitleField · DescriptionField · LabeledField · ChoiceChips · CardList · DateText · editingMenuActions
├── mutation/  mutationRejectionOf
├── dialog/    DialogController(open) · MutationDialogController · MutationDialog · DeletionDialog
└── photo/     rememberPhotoPicker (android · jvm · web · ios) · rememberPhotoBitmap

shared/ui/tasks                            (Step 3, now uses the pieces above)

shared/ui/goal
├── GoalsScreen                            public entry point: header + card list + dialogs
├── GoalsViewModel · GoalsScreenGraph      state + intents; DI composition root
├── model/     GoalDraft
├── component/ GoalPictureView · GOAL_EMOJI_PICTURES
├── card/      GoalCard
├── dialog/    GoalEditorDialog · GoalPictureField · GoalDeletionDialog
└── mock/      GoalsMockData
```

**Data flow**

* **Read:** `GoalsRepository.goalsState` → `GoalsViewModel.goalsState` (the same `StateFlow`; nothing is copied) → `GoalsScreen` → `CardList` → `GoalCard`. There is no loading state, because the core `StateFlow` always has a value.
* **Write:** card click or **Create goal** → `GoalDraft` → `MutationDialogController.submit()` → `GoalsManager` → success closes the dialog; a rejection shows inside it. Deletion: ⋮ → Delete → confirmation → `GoalsManager.deleteGoal`. Every screen updates from the core state; nothing refreshes by hand.
* **Photo:** **Choose photo** → `rememberPhotoPicker` (platform UI) → bytes → `GoalPicture.Photo` in the draft → saved to the core → `rememberPhotoBitmap` decodes and scales it for display.

**DRY map**

| Shared piece | Used by |
|---|---|
| `GoalRecord.PROGRESS_PERCENTAGE_RANGE` (core) | core validation, editor slider range and steps, card progress bar, “completed” color |
| `NullablePatch` / `nullablePatch` (`core:common`) | core picture and deadline patches; `GoalDraft` update requests |
| `GoalsBindings` (core) | `GoalsSessionGraph`, `GoalsScreenGraph` (production and tests) |
| `IdentifierGeneratorBindings` (`core:common`) | `GoalsScreenGraph`, `TasksScreenGraph`, `FinancialSessionGraph` |
| `LayoutWidth` + `screenPadding` (`ui:common`) | `GoalsScreen`, `TasksScreen` (and the social-battery plan) |
| `MutationDialogController` + `MutationDialog` + `mutationRejectionOf` | goal editor and deletion; task editor, deletion and reactions editor; task quick changes |
| `DeletionDialog` | goal deletion, task deletion |
| `TitleField`, `DescriptionField`, `LabeledField` | goal editor, task editor |
| `ChoiceChips` | goal emoji picker, task reactions (editor and rating popup) |
| `CardList` | goal list, task board columns |
| `DateText` | goal card deadline, task card and list due date |
| `editingMenuActions` | goal card ⋮, task card ⋮, task row ⋮ |
| `rememberPhotoPicker`, `rememberPhotoBitmap` | goal picture field and picture box; any future photo feature |
| Shared strings (Save, Edit, Delete, Title, Description, unsaved error, Choose photo) | Goals, Tasks |
| `GoalPictureView` | goal cards, editor preview |
| `AddButton`, `DateField`, `MoreActionsMenu`, `ControlledDialog`, `DAY_FORMAT` (existing `ui:common`) | Goals now, Tasks already |

---

## 4. Implementation steps

Each file shows **Old** (empty for a new file) and **New**. Small or heavily rewritten files show the whole file; larger files show only the changed regions, with two lines of context. Every block was copied from code that was compiled and tested (§5).

### Step 1 — Core: description, picture, date deadline, shared bindings (`shared/core/feature/goals`, `shared/core/feature/common`)

The mockup needs a **description** and an **icon or photo**, which the core doesn't have yet. The deadline becomes a date (`LocalDate`), like the Tasks due date, so the UI reuses the existing date field, format and text. All validation stays in the core.

#### `shared/core/feature/goals/build.gradle.kts` — modified

`LocalDate` for the deadline (the same dependency as the tasks core).

**Old:**

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "goals"; isStatic = true } }
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    android {
       namespace = "org.orev.nahidka.feature.goals"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core:feature:common"))
            api(libs.kotlinx.coroutines.core)
            implementation(libs.metro.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
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
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "goals"; isStatic = true } }
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    android {
       namespace = "org.orev.nahidka.feature.goals"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core:feature:common"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.metro.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/dto/GoalPicture.kt` — new

The goal picture: an emoji or photo bytes. Photos compare by **content**, so saving the same photo again is not a change.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.goals.dto

sealed interface GoalPicture {

    data class Emoji(val symbol: String) : GoalPicture

    class Photo(val content: ByteArray) : GoalPicture {

        override fun equals(other: Any?): Boolean = other is Photo && content.contentEquals(other.content)

        override fun hashCode(): Int = content.contentHashCode()
    }
}
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/dto/GoalRecord.kt` — modified

Adds `description` and `picture`; `deadlineInstant: Instant?` → `deadlineDate: LocalDate?`. `PROGRESS_PERCENTAGE_RANGE` is the single source of `0..100`; core validation, the slider and the progress bar all read it.

**Old:**

```kotlin
package org.orev.nahidka.feature.goals.dto

import kotlin.time.Instant

data class GoalRecord(
    val identifier: String,
    val title: String,
    val progressPercentage: Float = 0f,
    val deadlineInstant: Instant? = null,
)
```

**New:**

```kotlin
package org.orev.nahidka.feature.goals.dto

import kotlinx.datetime.LocalDate

data class GoalRecord(
    val identifier: String,
    val title: String,
    val description: String = "",
    val picture: GoalPicture? = null,
    val progressPercentage: Float = 0f,
    val deadlineDate: LocalDate? = null,
) {

    companion object {
        val PROGRESS_PERCENTAGE_RANGE = 0f..100f
    }
}
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/dto/GoalCreationRequest.kt` — modified

Mirrors the record.

**Old:**

```kotlin
package org.orev.nahidka.feature.goals.dto

import kotlin.time.Instant

data class GoalCreationRequest(
    val identifier: String,
    val title: String,
    val progressPercentage: Float = 0f,
    val deadlineInstant: Instant? = null
)
```

**New:**

```kotlin
package org.orev.nahidka.feature.goals.dto

import kotlinx.datetime.LocalDate

data class GoalCreationRequest(
    val identifier: String,
    val title: String,
    val description: String = "",
    val picture: GoalPicture? = null,
    val progressPercentage: Float = 0f,
    val deadlineDate: LocalDate? = null
)
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/dto/GoalUpdateRequest.kt` — modified

`description` works like `title` (`null` keeps it). The picture and deadline are optional, so they use `NullablePatch` (keep / set / clear).

**Old:**

```kotlin
package org.orev.nahidka.feature.goals.dto

import org.orev.nahidka.core.common.NullablePatch
import kotlin.time.Instant

data class GoalUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val progressPercentage: Float? = null,
    val deadlinePatch: NullablePatch<Instant> = NullablePatch.Keep
)
```

**New:**

```kotlin
package org.orev.nahidka.feature.goals.dto

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch

data class GoalUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val description: String? = null,
    val picturePatch: NullablePatch<GoalPicture> = NullablePatch.Keep,
    val progressPercentage: Float? = null,
    val deadlinePatch: NullablePatch<LocalDate> = NullablePatch.Keep
)
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/service/GoalsContext.kt` — modified

Copies and patches the new fields. Validation uses `PROGRESS_PERCENTAGE_RANGE` (it also rejects `NaN` and infinities, so `isFinite()` is no longer needed) and rejects a blank emoji or an empty photo.

_Change 1 of 3 (old line 35 / new line 35)_

**Old:**

```kotlin
    override suspend fun createGoal(request: GoalCreationRequest): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val goal = GoalRecord(request.identifier, request.title, request.progressPercentage, request.deadlineInstant)

        validateGoal(goal)
```

**New:**

```kotlin
    override suspend fun createGoal(request: GoalCreationRequest): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val goal = GoalRecord(
            request.identifier,
            request.title,
            request.description,
            request.picture,
            request.progressPercentage,
            request.deadlineDate
        )

        validateGoal(goal)
```

_Change 2 of 3 (old line 58 / new line 65)_

**Old:**

```kotlin
        val currentGoal = previousGoal.copy(
            title = request.title ?: previousGoal.title,
            progressPercentage = request.progressPercentage ?: previousGoal.progressPercentage,
            deadlineInstant = request.deadlinePatch.applyTo(previousGoal.deadlineInstant)
        )
```

**New:**

```kotlin
        val currentGoal = previousGoal.copy(
            title = request.title ?: previousGoal.title,
            description = request.description ?: previousGoal.description,
            picture = request.picturePatch.applyTo(previousGoal.picture),
            progressPercentage = request.progressPercentage ?: previousGoal.progressPercentage,
            deadlineDate = request.deadlinePatch.applyTo(previousGoal.deadlineDate)
        )
```

_Change 3 of 3 (old line 89 / new line 98)_

**Old:**

```kotlin
        require(goal.identifier.isNotBlank()) { "Goal identifier must not be blank" }
        require(goal.title.isNotBlank()) { "Goal title must not be blank" }
        require(goal.progressPercentage.isFinite() && goal.progressPercentage in 0f..100f) { "Goal progress must be between 0 and 100" }
    }
}
```

**New:**

```kotlin
        require(goal.identifier.isNotBlank()) { "Goal identifier must not be blank" }
        require(goal.title.isNotBlank()) { "Goal title must not be blank" }
        require(goal.progressPercentage in GoalRecord.PROGRESS_PERCENTAGE_RANGE) { "Goal progress must be between 0 and 100" }

        when (val picture = goal.picture) {
            is GoalPicture.Emoji -> require(picture.symbol.isNotBlank()) { "Goal emoji must not be blank" }
            is GoalPicture.Photo -> require(picture.content.isNotEmpty()) { "Goal photo must not be empty" }
            null -> Unit
        }
    }
}
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/service/GoalsManager.kt` — modified

Class-level `@Inject`, which removes the existing Metro warning (“Consider moving the annotation to the class”).

**Old:**

```kotlin
package org.orev.nahidka.feature.goals.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult

@SingleIn(GoalsSessionScope::class)
class GoalsManager @Inject constructor(private val goalsRepository: GoalsRepository) {

    suspend fun createGoal(goalCreationRequest: GoalCreationRequest): GoalsMutationResult =
        goalsRepository.createGoal(goalCreationRequest)

    suspend fun deleteGoal(identifier: String): GoalsMutationResult =
        goalsRepository.deleteGoal(identifier)

    suspend fun updateGoal(goalUpdateRequest: GoalUpdateRequest): GoalsMutationResult =
        goalsRepository.updateGoal(goalUpdateRequest)
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.goals.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult

@Inject
@SingleIn(GoalsSessionScope::class)
class GoalsManager(private val goalsRepository: GoalsRepository) {

    suspend fun createGoal(goalCreationRequest: GoalCreationRequest): GoalsMutationResult =
        goalsRepository.createGoal(goalCreationRequest)

    suspend fun deleteGoal(identifier: String): GoalsMutationResult =
        goalsRepository.deleteGoal(identifier)

    suspend fun updateGoal(goalUpdateRequest: GoalUpdateRequest): GoalsMutationResult =
        goalsRepository.updateGoal(goalUpdateRequest)
}
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/di/GoalsBindings.kt` — new

Metro rejects one graph extending another, so the providers move into a `@BindingContainer`, as `TasksBindings` did. `GoalsSessionGraph` and the new `GoalsScreenGraph` share it.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.goals.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsRepository

@BindingContainer
object GoalsBindings {

    @Provides
    private fun provideGoalsRepository(goalsContext: GoalsContext): GoalsRepository = goalsContext

    @Provides
    @SingleIn(GoalsSessionScope::class)
    private fun provideGoalsContext(): GoalsContext = GoalsContext()
}
```

#### `shared/core/feature/goals/src/commonMain/kotlin/org/orev/nahidka/feature/goals/di/GoalsSessionGraph.kt` — modified

Uses `GoalsBindings` instead of its own companion providers.

**Old:**

```kotlin
package org.orev.nahidka.feature.goals.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.goals.service.GoalsRepository

@DependencyGraph(GoalsSessionScope::class)
interface GoalsSessionGraph {
    val goalsContext: GoalsContext
    val goalsManager: GoalsManager

    companion object {
        @Provides
        private fun provideGoalsRepository(context: GoalsContext): GoalsRepository = context

        @Provides
        @SingleIn(GoalsSessionScope::class)
        private fun provideGoalsContext(): GoalsContext = GoalsContext()
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.goals.di

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager

@DependencyGraph(GoalsSessionScope::class, bindingContainers = [GoalsBindings::class])
interface GoalsSessionGraph {
    val goalsContext: GoalsContext
    val goalsManager: GoalsManager
}
```

#### `shared/core/feature/goals/src/commonTest/kotlin/org/orev/nahidka/feature/goals/service/GoalsContextTest.kt` — new

The module had no tests. These cover the new fields, clearing through patches, photo content equality, and rejected input.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.goals.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import kotlin.test.*

private const val GOAL_IDENTIFIER = "half-marathon"

class GoalsContextTest {

    private val goalsContext = GoalsContext()

    @Test
    fun createdGoalKeepsDescriptionPictureAndDeadline() = runTest {
        val deadlineDate = LocalDate(2027, 4, 18)

        goalsContext.createGoal(
            GoalCreationRequest(
                identifier = GOAL_IDENTIFIER,
                title = "Run a half marathon",
                description = "Finish 21 km",
                picture = GoalPicture.Emoji("🏃"),
                progressPercentage = 40f,
                deadlineDate = deadlineDate,
            ),
        )

        val createdGoal = goalsContext
            .currentSnapshot()
            .goals
            .single()
        assertEquals("Finish 21 km", createdGoal.description)
        assertEquals(GoalPicture.Emoji("🏃"), createdGoal.picture)
        assertEquals(deadlineDate, createdGoal.deadlineDate)
    }

    @Test
    fun updateReplacesAndClearsOptionalFields() = runTest {
        createGoal(GoalPicture.Emoji("🏃"))

        goalsContext.updateGoal(
            GoalUpdateRequest(
                identifier = GOAL_IDENTIFIER,
                description = "Finish under two hours",
                picturePatch = NullablePatch.Set(GoalPicture.Photo(byteArrayOf(1, 2, 3))),
                deadlinePatch = NullablePatch.Set(LocalDate(2027, 4, 18)),
            ),
        )
        goalsContext.updateGoal(
            GoalUpdateRequest(
                identifier = GOAL_IDENTIFIER,
                picturePatch = NullablePatch.Clear,
                deadlinePatch = NullablePatch.Clear,
            ),
        )

        val updatedGoal = goalsContext
            .currentSnapshot()
            .goals
            .single()
        assertEquals("Finish under two hours", updatedGoal.description)
        assertNull(updatedGoal.picture)
        assertNull(updatedGoal.deadlineDate)
    }

    @Test
    fun samePhotoContentIsNotAChange() = runTest {
        createGoal(GoalPicture.Photo(byteArrayOf(1, 2, 3)))

        val mutationResult = goalsContext.updateGoal(
            GoalUpdateRequest(
                identifier = GOAL_IDENTIFIER,
                picturePatch = NullablePatch.Set(GoalPicture.Photo(byteArrayOf(1, 2, 3))),
            ),
        )

        assertFalse(mutationResult.changed)
        assertEquals(1L, goalsContext.currentSnapshot().revision)
    }

    @Test
    fun blankEmojiEmptyPhotoAndOutOfRangeProgressAreRejected() = runTest {
        assertFailsWith<IllegalArgumentException> { createGoal(GoalPicture.Emoji(" ")) }
        assertFailsWith<IllegalArgumentException> { createGoal(GoalPicture.Photo(byteArrayOf())) }
        assertFailsWith<IllegalArgumentException> {
            goalsContext.createGoal(GoalCreationRequest(GOAL_IDENTIFIER, "Run", progressPercentage = 101f))
        }

        assertTrue(
            goalsContext
                .currentSnapshot()
                .goals
                .isEmpty(),
        )
    }

    private suspend fun createGoal(picture: GoalPicture) {
        goalsContext.createGoal(GoalCreationRequest(GOAL_IDENTIFIER, "Run a half marathon", picture = picture))
    }
}
```

#### `shared/core/feature/goals/README.md` — modified

Updated for the new fields and `GoalsBindings`. The main example was executed as a temporary test (it passes) and then removed.

**Old:**

~~~~markdown
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
~~~~

**New:**

~~~~markdown
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
~~~~

#### `shared/core/feature/common/src/commonMain/kotlin/org/orev/nahidka/core/common/IdentifierGeneratorBindings.kt` — new

`provideIdentifierGenerator()` was already written twice (`TasksScreenGraph`, `FinancialSessionGraph`), and `GoalsScreenGraph` would make three. Each graph now includes this container instead.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.core.common

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object IdentifierGeneratorBindings {

    @Provides
    private fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
```

### Step 2 — Shared UI (`shared/ui/common`): pieces Goals would otherwise copy from Tasks, plus the photo picker

Everything here is feature-agnostic. Each piece either comes from Tasks, which Goals would otherwise copy, or is new and generic, such as the photo picker. Step 3 switches Tasks over without changing its behavior.

#### `shared/ui/common/build.gradle.kts` — modified

`activity-compose` for the Android Photo Picker and `kotlin-browser` (already used by `webApp`) for the web file input. **`publicResClass = true`** lets feature modules use shared strings such as Save, Delete, Title and Description instead of keeping their own copies.

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
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
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
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        webMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    publicResClass = true
}
```

#### `shared/ui/common/src/commonMain/composeResources/values/strings.xml` — modified

Shared labels (formerly `tasks_action_save`, `tasks_field_title`, …) and the photo action.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Cancel</string>
    <string name="common_action_select">Select</string>
    <string name="common_action_clear">Clear</string>
    <string name="common_action_more">More actions</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Cancel</string>
    <string name="common_action_select">Select</string>
    <string name="common_action_clear">Clear</string>
    <string name="common_action_more">More actions</string>
    <string name="common_action_save">Save</string>
    <string name="common_action_edit">Edit</string>
    <string name="common_action_delete">Delete</string>
    <string name="common_action_choose_photo">Choose photo</string>
    <string name="common_field_title">Title</string>
    <string name="common_field_description">Description</string>
    <string name="common_error_unsaved">This change could not be saved</string>
</resources>
```

#### `shared/ui/common/src/commonMain/composeResources/values-uk/strings.xml` — modified

Ukrainian, using the same wording Tasks already used.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Скасувати</string>
    <string name="common_action_select">Вибрати</string>
    <string name="common_action_clear">Очистити</string>
    <string name="common_action_more">Інші дії</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Скасувати</string>
    <string name="common_action_select">Вибрати</string>
    <string name="common_action_clear">Очистити</string>
    <string name="common_action_more">Інші дії</string>
    <string name="common_action_save">Зберегти</string>
    <string name="common_action_edit">Редагувати</string>
    <string name="common_action_delete">Видалити</string>
    <string name="common_action_choose_photo">Вибрати фото</string>
    <string name="common_field_title">Назва</string>
    <string name="common_field_description">Опис</string>
    <string name="common_error_unsaved">Не вдалося зберегти зміни</string>
</resources>
```

#### `shared/ui/common/src/commonMain/composeResources/values-ru/strings.xml` — modified

Russian, using the same wording Tasks already used.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Отмена</string>
    <string name="common_action_select">Выбрать</string>
    <string name="common_action_clear">Очистить</string>
    <string name="common_action_more">Другие действия</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Отмена</string>
    <string name="common_action_select">Выбрать</string>
    <string name="common_action_clear">Очистить</string>
    <string name="common_action_more">Другие действия</string>
    <string name="common_action_save">Сохранить</string>
    <string name="common_action_edit">Редактировать</string>
    <string name="common_action_delete">Удалить</string>
    <string name="common_action_choose_photo">Выбрать фото</string>
    <string name="common_field_title">Название</string>
    <string name="common_field_description">Описание</string>
    <string name="common_error_unsaved">Не удалось сохранить изменения</string>
</resources>
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/layout/LayoutWidth.kt` — modified

Each width class carries its screen padding (16 / 24 dp). **This change is identical to Step 2 of the pending social-battery plan.** Whichever plan lands first applies it, and the other skips it.

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

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/TitleField.kt` — new

The title field from the Tasks editor. The Goals editor uses the same one.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_field_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun TitleField(title: String, onTitleChange: (String) -> Unit) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.common_field_title)) },
        singleLine = true,
    )
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/DescriptionField.kt` — new

The 3–6 line description field from the Tasks editor, including its line limits.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_field_description
import org.jetbrains.compose.resources.stringResource

private const val DESCRIPTION_MINIMUM_LINES = 3
private const val DESCRIPTION_MAXIMUM_LINES = 6

@Composable
fun DescriptionField(description: String, onDescriptionChange: (String) -> Unit) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.common_field_description)) },
        minLines = DESCRIPTION_MINIMUM_LINES,
        maxLines = DESCRIPTION_MAXIMUM_LINES,
    )
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/LabeledField.kt` — new

A small caption above a control, taken from the rating section of the Tasks editor. Goals uses it for its picture and progress sections.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/ChoiceChips.kt` — new

The chip row from `TaskRatingReactions`, made generic. Tapping the selected chip clears it. It shows the rating reactions in Tasks and the emoji pictures in Goals.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <Option> ChoiceChips(
    options: List<Option>,
    selectedOption: Option?,
    optionTitle: @Composable (Option) -> String,
    onOptionSelect: (Option?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option == selectedOption

            FilterChip(
                selected = selected,
                onClick = { onOptionSelect(option.takeUnless { selected }) },
                label = { Text(optionTitle(option), style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/CardList.kt` — new

The lazy card list with an empty message, from `TaskCardList`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun <Item> CardList(
    listItems: List<Item>,
    itemKey: (Item) -> Any,
    emptyListMessage: String,
    modifier: Modifier = Modifier,
    itemCard: @Composable (Item) -> Unit,
) {
    if (listItems.isEmpty()) {
        Text(
            text = emptyListMessage,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listItems, key = itemKey) { listItem ->
                itemCard(listItem)
            }
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/DateText.kt` — new

`📅 dd.MM.yyyy`, moved unchanged from `TaskDueDateText`. It shows the due date on task cards and the deadline on goal cards.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import org.orev.nahidka.ui.common.format.DAY_FORMAT

@Composable
fun DateText(date: LocalDate, modifier: Modifier = Modifier) {
    Text(
        text = "📅 ${date.format(DAY_FORMAT)}",
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/EditingMenuActions.kt` — new

The ⋮ “Edit / Delete” pair used by task cards, task rows and goal cards.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_delete
import nahidka.shared.ui.common.generated.resources.common_action_edit
import org.jetbrains.compose.resources.stringResource

@Composable
fun editingMenuActions(onEdit: () -> Unit, onDelete: () -> Unit): List<MenuAction> = listOf(
    MenuAction(stringResource(Res.string.common_action_edit), onEdit),
    MenuAction(stringResource(Res.string.common_action_delete), onDelete),
)
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/mutation/MutationRejection.kt` — new

The `rejectionOf` helper from `TasksViewModel`: a core mutation that fails with `IllegalArgumentException` (every core's `require`) becomes a value instead of a crash.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.mutation

suspend fun mutationRejectionOf(mutation: suspend () -> Unit): IllegalArgumentException? =
    try {
        mutation()
        null
    } catch (rejection: IllegalArgumentException) {
        rejection
    }
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/DialogController.kt` — modified

`open`, so `MutationDialogController` can configure it. Nothing else changes.

_Change 1 of 1 (old line 8 / new line 8)_

**Old:**

```kotlin
import kotlinx.coroutines.launch

class DialogController<Draft, Rejection : Any>(
    private val coroutineScope: CoroutineScope,
    private val draftValidation: (Draft) -> Boolean = { true },
```

**New:**

```kotlin
import kotlinx.coroutines.launch

open class DialogController<Draft, Rejection : Any>(
    private val coroutineScope: CoroutineScope,
    private val draftValidation: (Draft) -> Boolean = { true },
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/MutationDialogController.kt` — new

Replaces the `TaskDialogController` typealias and the `rejectionOf { … }` wrapping repeated at every controller. Callers pass only the mutation.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.CoroutineScope
import org.orev.nahidka.ui.common.mutation.mutationRejectionOf

class MutationDialogController<Draft>(
    coroutineScope: CoroutineScope,
    draftValidation: (Draft) -> Boolean = { true },
    mutation: suspend (Draft) -> Unit,
) : DialogController<Draft, IllegalArgumentException>(
    coroutineScope = coroutineScope,
    draftValidation = draftValidation,
    submission = { draft -> mutationRejectionOf { mutation(draft) } },
)
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/MutationDialog.kt` — new

Moved from `TaskDialog`: `ControlledDialog` with the shared “This change could not be saved” message.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_error_unsaved
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Draft> MutationDialog(
    dialogController: MutationDialogController<Draft>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    content: @Composable (Draft) -> Unit,
) {
    ControlledDialog(
        dialogController = dialogController,
        title = title,
        confirmationTitle = confirmationTitle,
        rejectionText = { stringResource(Res.string.common_error_unsaved) },
        content = content,
    )
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/DeletionDialog.kt` — new

The shared shape of a deletion confirmation (title, message, **Delete**). The title and message stay per feature because Ukrainian and Russian word forms differ: “Завдання … буде видалено” / “Ціль … буде видалено”.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_delete
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Item> DeletionDialog(
    deletion: MutationDialogController<Item>,
    title: String,
    message: @Composable (Item) -> String,
) {
    MutationDialog(
        dialogController = deletion,
        title = { title },
        confirmationTitle = stringResource(Res.string.common_action_delete),
    ) { item ->
        Text(message(item))
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/photo/PhotoPicker.kt` — new

Returns a function that opens the platform photo picker and delivers the chosen file's bytes. Cancelling delivers nothing.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit
```

#### `shared/ui/common/src/androidMain/kotlin/org/orev/nahidka/ui/common/photo/PhotoPicker.android.kt` — new

The system **Photo Picker** (`PickVisualMedia`, images only). It needs no storage permission. Reading happens on `Dispatchers.IO`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.photo

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val contentResolver = LocalContext.current.contentResolver
    val coroutineScope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photoUri ->
        photoUri?.let { pickedPhotoUri ->
            coroutineScope.launch {
                contentResolver
                    .readPhoto(pickedPhotoUri)
                    ?.let(onPhotoPick)
            }
        }
    }

    return {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private suspend fun ContentResolver.readPhoto(photoUri: Uri): ByteArray? =
    withContext(Dispatchers.IO) {
        openInputStream(photoUri)?.use(InputStream::readBytes)
    }
```

#### `shared/ui/common/src/jvmMain/kotlin/org/orev/nahidka/ui/common/photo/PhotoPicker.jvm.kt` — new

The native AWT `FileDialog`, filtered to formats Skia can decode. Reading happens on `Dispatchers.IO`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_choose_photo
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

private val PHOTO_FILE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val photoPickerTitle = stringResource(Res.string.common_action_choose_photo)
    val coroutineScope = rememberCoroutineScope()

    return {
        choosePhotoFile(photoPickerTitle)?.let { photoFile ->
            coroutineScope.launch {
                onPhotoPick(withContext(Dispatchers.IO) { photoFile.readBytes() })
            }
        }
    }
}

private fun choosePhotoFile(photoPickerTitle: String): File? =
    FileDialog(null as Frame?, photoPickerTitle, FileDialog.LOAD)
        .apply {
            isMultipleMode = false
            filenameFilter = FilenameFilter { _, fileName ->
                fileName
                    .substringAfterLast('.')
                    .lowercase() in PHOTO_FILE_EXTENSIONS
            }
            isVisible = true
        }
        .files
        .firstOrNull()
```

#### `shared/ui/common/src/webMain/kotlin/org/orev/nahidka/ui/common/photo/PhotoPicker.web.kt` — new

One source for **JS and Wasm**: a hidden `<input type=file accept=image/…>`, read with `Blob.arrayBuffer()`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import js.buffer.toByteArray
import kotlinx.coroutines.launch
import web.blob.arrayBuffer
import web.dom.document
import web.events.EventHandler
import web.events.addHandler
import web.html.HtmlTagName
import web.html.InputType
import web.html.changeEvent
import web.html.file

private const val PHOTO_MEDIA_TYPES = "image/png,image/jpeg,image/webp,image/gif,image/bmp"

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val coroutineScope = rememberCoroutineScope()

    return {
        val photoInput = document.createElement(HtmlTagName.input)
        photoInput.type = InputType.file
        photoInput.accept = PHOTO_MEDIA_TYPES
        photoInput.changeEvent.addHandler(
            EventHandler {
                photoInput.files
                    ?.item(0)
                    ?.let { photoFile ->
                        coroutineScope.launch {
                            onPhotoPick(
                                photoFile
                                    .arrayBuffer()
                                    .toByteArray(),
                            )
                        }
                    }
            },
        )
        photoInput.click()
    }
}
```

#### `shared/ui/common/src/iosMain/kotlin/org/orev/nahidka/ui/common/photo/PhotoPicker.ios.kt` — new

`PHPickerViewController`, presented from Compose's `LocalUIViewController`. The image is re-encoded as JPEG, so HEIC photos also display. **This compiles for `iosArm64` and `iosSimulatorArm64` through klib cross-compilation, but it has not run on a device (§5).**

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private const val PHOTO_JPEG_COMPRESSION_QUALITY = 0.9

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val presentingViewController = LocalUIViewController.current
    val currentOnPhotoPick by rememberUpdatedState(onPhotoPick)
    val photoPickerDelegate = remember { PhotoPickerDelegate { photoContent -> currentOnPhotoPick(photoContent) } }

    return {
        val photoPickerConfiguration = PHPickerConfiguration()
            .apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
        val photoPickerViewController = PHPickerViewController(photoPickerConfiguration)
            .apply { delegate = photoPickerDelegate }

        presentingViewController.presentViewController(photoPickerViewController, animated = true, completion = null)
    }
}

private class PhotoPickerDelegate(
    private val onPhotoPick: (ByteArray) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        (didFinishPicking.firstOrNull() as? PHPickerResult)
            ?.itemProvider
            ?.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { pickedPhotoData, _ ->
                pickedPhotoData
                    ?.let { photoData -> UIImage(data = photoData) }
                    ?.let { pickedPhoto -> UIImageJPEGRepresentation(pickedPhoto, PHOTO_JPEG_COMPRESSION_QUALITY) }
                    ?.toByteArray()
                    ?.let { photoContent ->
                        dispatch_async(dispatch_get_main_queue()) { onPhotoPick(photoContent) }
                    }
            }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        usePinned { pinnedPhotoContent -> memcpy(pinnedPhotoContent.addressOf(0), bytes, length) }
    }
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/photo/PhotoBitmap.kt` — new

Decodes photo bytes **off the main thread** and scales them to at most 512 px. A 12-megapixel photo would otherwise keep ~48 MB per visible card. Unreadable bytes yield `null`, which leaves the picture box empty.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.roundToInt

private const val PHOTO_BITMAP_MAXIMUM_SIDE = 512

@Composable
fun rememberPhotoBitmap(photoContent: ByteArray): ImageBitmap? =
    produceState<ImageBitmap?>(null, photoContent) {
        value = withContext(Dispatchers.Default) { decodePhotoBitmap(photoContent) }
    }.value

private fun decodePhotoBitmap(photoContent: ByteArray): ImageBitmap? =
    runCatching { photoContent.decodeToImageBitmap() }
        .getOrNull()
        ?.scaledDown()

private fun ImageBitmap.scaledDown(): ImageBitmap {
    val scale = PHOTO_BITMAP_MAXIMUM_SIDE.toFloat() / maxOf(width, height)

    if (scale >= 1f) {
        return this
    }

    val scaledSize = IntSize(
        width = (width * scale)
            .roundToInt()
            .coerceAtLeast(1),
        height = (height * scale)
            .roundToInt()
            .coerceAtLeast(1),
    )
    val scaledBitmap = ImageBitmap(scaledSize.width, scaledSize.height)

    Canvas(scaledBitmap).drawImageRect(
        image = this,
        dstSize = scaledSize,
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )

    return scaledBitmap
}
```

#### `shared/ui/common/src/commonTest/kotlin/org/orev/nahidka/ui/common/dialog/MutationDialogControllerTest.kt` — new

A rejection keeps the dialog open with the error; a success closes it.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MutationDialogControllerTest {

    @Test
    fun rejectedMutationKeepsDialogOpenWithRejection() = runTest {
        val rejection = IllegalArgumentException("Title must not be blank")
        val mutationDialogController = MutationDialogController<String>(this) { throw rejection }

        mutationDialogController.open("Write plan")
        mutationDialogController.submit()
        advanceUntilIdle()

        assertEquals(rejection, checkNotNull(mutationDialogController.dialogState.value).rejection)
    }

    @Test
    fun acceptedMutationClosesDialog() = runTest {
        val mutatedDrafts = mutableListOf<String>()
        val mutationDialogController = MutationDialogController<String>(this) { draft -> mutatedDrafts.add(draft) }

        mutationDialogController.open("Write plan")
        mutationDialogController.submit()
        advanceUntilIdle()

        assertEquals(listOf("Write plan"), mutatedDrafts)
        assertNull(mutationDialogController.dialogState.value)
    }
}
```

### Step 3 — Tasks adopt the shared pieces (`shared/ui/tasks`, no behavior change)

Without this step, `ui/common` would hold a second copy of every piece above. All 20 tasks UI tests (board drag with mouse and touch, reactions, editor, deletion, rendered layouts) and `TasksHostTest` pass unchanged. English texts are identical, so no test text changed.

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksScreen.kt` — modified

Shared padding and the shared “could not be saved” snackbar text.

_Change 1 of 4 (old line 10 / new line 10)_

**Old:**

```kotlin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
```

**New:**

```kotlin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.common.generated.resources.common_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
```

_Change 2 of 4 (old line 19 / new line 18)_

**Old:**

```kotlin
import org.orev.nahidka.ui.tasks.dialog.TaskRatingLevelsDialog
import org.orev.nahidka.ui.tasks.list.TasksList

@Composable
```

**New:**

```kotlin
import org.orev.nahidka.ui.tasks.dialog.TaskRatingLevelsDialog
import org.orev.nahidka.ui.tasks.list.TasksList
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

@Composable
```

_Change 3 of 4 (old line 25 / new line 25)_

**Old:**

```kotlin
    var selectedView by rememberSaveable { mutableStateOf(TasksView.BOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val changeRejectionMessage = stringResource(Res.string.tasks_error_unsaved)

    LaunchedEffect(tasksViewModel, changeRejectionMessage) {
```

**New:**

```kotlin
    var selectedView by rememberSaveable { mutableStateOf(TasksView.BOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val changeRejectionMessage = stringResource(CommonResources.string.common_error_unsaved)

    LaunchedEffect(tasksViewModel, changeRejectionMessage) {
```

_Change 4 of 4 (old line 37 / new line 37)_

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksScreenGraph.kt` — modified

`IdentifierGeneratorBindings` instead of its own provider.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.feature.tasks.di.TasksBindings
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.service.TasksManager

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class])
interface TasksScreenGraph {
    val tasksViewModel: TasksViewModel
    val tasksManager: TasksManager

    @Provides
    fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.core.common.IdentifierGeneratorBindings
import org.orev.nahidka.feature.tasks.di.TasksBindings
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.service.TasksManager

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class, IdentifierGeneratorBindings::class])
interface TasksScreenGraph {
    val tasksViewModel: TasksViewModel
    val tasksManager: TasksManager
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksViewModel.kt` — modified

`MutationDialogController` and `mutationRejectionOf`. The private `rejectionOf` is gone.

_Change 1 of 4 (old line 12 / new line 12)_

**Old:**

```kotlin
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.service.TasksRepository
import org.orev.nahidka.ui.tasks.dialog.TaskDialogController
import org.orev.nahidka.ui.tasks.model.*
```

**New:**

```kotlin
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.service.TasksRepository
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.common.mutation.mutationRejectionOf
import org.orev.nahidka.ui.tasks.model.*
```

_Change 2 of 4 (old line 36 / new line 37)_

**Old:**

```kotlin
    val changeRejections: SharedFlow<IllegalArgumentException> = mutableChangeRejections.asSharedFlow()

    val taskEditor = TaskDialogController<TaskDraft>(viewModelScope, TaskDraft::submittable) { taskDraft ->
        rejectionOf { saveTask(taskDraft) }
    }

    val taskDeletion = TaskDialogController<TaskItem>(viewModelScope) { taskItem ->
        rejectionOf { tasksManager.deleteTasks(listOf(taskItem.task.identifier)) }
    }

    val ratingLevelsEditor = TaskDialogController<TaskRatingLevelsDraft>(
        viewModelScope,
        TaskRatingLevelsDraft::submittable,
    ) { ratingLevelsDraft ->
        rejectionOf { tasksManager.replaceRatingLevels(ratingLevelsDraft.ratingLevels) }
    }
```

**New:**

```kotlin
    val changeRejections: SharedFlow<IllegalArgumentException> = mutableChangeRejections.asSharedFlow()

    val taskEditor = MutationDialogController<TaskDraft>(viewModelScope, TaskDraft::submittable) { taskDraft ->
        saveTask(taskDraft)
    }

    val taskDeletion = MutationDialogController<TaskItem>(viewModelScope) { taskItem ->
        tasksManager.deleteTasks(listOf(taskItem.task.identifier))
    }

    val ratingLevelsEditor = MutationDialogController<TaskRatingLevelsDraft>(
        viewModelScope,
        TaskRatingLevelsDraft::submittable,
    ) { ratingLevelsDraft ->
        tasksManager.replaceRatingLevels(ratingLevelsDraft.ratingLevels)
    }
```

_Change 3 of 4 (old line 93 / new line 94)_

**Old:**

```kotlin
    private fun changeTask(taskUpdateRequest: TaskUpdateRequest) {
        viewModelScope.launch {
            rejectionOf { tasksManager.updateTasks(listOf(taskUpdateRequest)) }
                ?.let(mutableChangeRejections::tryEmit)
        }
```

**New:**

```kotlin
    private fun changeTask(taskUpdateRequest: TaskUpdateRequest) {
        viewModelScope.launch {
            mutationRejectionOf { tasksManager.updateTasks(listOf(taskUpdateRequest)) }
                ?.let(mutableChangeRejections::tryEmit)
        }
```

_Change 4 of 4 (old line 107 / new line 108)_

**Old:**

```kotlin
        }
    }

    private suspend fun rejectionOf(tasksMutation: suspend () -> TasksMutationResult): IllegalArgumentException? =
        try {
            tasksMutation()
            null
        } catch (rejection: IllegalArgumentException) {
            rejection
        }
}
```

**New:**

```kotlin
        }
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskDialogController.kt` — deleted

Replaced by `MutationDialogController`.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import org.orev.nahidka.ui.common.dialog.DialogController

typealias TaskDialogController<Draft> = DialogController<Draft, IllegalArgumentException>
```

**New:** _(file deleted — `MutationDialogController` in `shared/ui/common` replaces this typealias and also owns the rejection mapping)_

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskDialog.kt` — deleted

Moved to `ui/common` as `MutationDialog`.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.dialog.ControlledDialog

@Composable
internal fun <Draft> TaskDialog(
    dialogController: TaskDialogController<Draft>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    content: @Composable (Draft) -> Unit,
) {
    ControlledDialog(
        dialogController = dialogController,
        title = title,
        confirmationTitle = confirmationTitle,
        rejectionText = { stringResource(Res.string.tasks_error_unsaved) },
        content = content,
    )
}
```

**New:** _(file deleted — moved to `shared/ui/common/.../dialog/MutationDialog.kt`, see Step 2)_

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskEditorDialog.kt` — modified

`MutationDialog`, `TitleField`, `DescriptionField` and `LabeledField`. Rewritten in several places, so the whole file is shown.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.ChoiceField
import org.orev.nahidka.ui.common.component.DateField
import org.orev.nahidka.ui.tasks.component.TaskRatingReactions
import org.orev.nahidka.ui.tasks.component.title
import org.orev.nahidka.ui.tasks.model.TaskDraft

private const val TASK_DESCRIPTION_MINIMUM_LINES = 3
private const val TASK_DESCRIPTION_MAXIMUM_LINES = 6

@Composable
internal fun TaskEditorDialog(
    taskEditor: TaskDialogController<TaskDraft>,
    ratingLevels: List<TaskRatingLevel>,
) {
    TaskDialog(
        dialogController = taskEditor,
        title = { taskDraft ->
            stringResource(
                if (taskDraft.editedTask == null) Res.string.tasks_creation_title else Res.string.tasks_editing_title,
            )
        },
        confirmationTitle = stringResource(Res.string.tasks_action_save),
    ) { taskDraft ->
        TaskForm(taskDraft, ratingLevels, taskEditor::edit)
    }
}

@Composable
private fun TaskForm(
    taskDraft: TaskDraft,
    ratingLevels: List<TaskRatingLevel>,
    onTaskDraftEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    OutlinedTextField(
        value = taskDraft.title,
        onValueChange = { title -> onTaskDraftEdit { draft -> draft.copy(title = title) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.tasks_field_title)) },
        singleLine = true,
    )
    OutlinedTextField(
        value = taskDraft.description,
        onValueChange = { description -> onTaskDraftEdit { draft -> draft.copy(description = description) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.tasks_field_description)) },
        minLines = TASK_DESCRIPTION_MINIMUM_LINES,
        maxLines = TASK_DESCRIPTION_MAXIMUM_LINES,
    )
    ChoiceField(
        title = stringResource(Res.string.tasks_field_status),
        options = TaskStatus.entries,
        selectedOption = taskDraft.status,
        optionTitle = { status -> stringResource(status.title) },
        onOptionSelect = { status -> onTaskDraftEdit { draft -> draft.withStatus(status) } },
    )
    DateField(
        date = taskDraft.dueDate,
        title = stringResource(Res.string.tasks_field_due_date),
        onDateSelect = { dueDate -> onTaskDraftEdit { draft -> draft.copy(dueDate = dueDate) } },
        onDateClear = { onTaskDraftEdit { draft -> draft.copy(dueDate = null) } },
    )
    if (taskDraft.status.acceptsRating && ratingLevels.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.tasks_field_rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TaskRatingReactions(
                ratingLevels = ratingLevels,
                selectedRatingLevel = taskDraft.ratingLevel,
                onRatingLevelSelect = { ratingLevel ->
                    onTaskDraftEdit { draft -> draft.copy(ratingLevel = ratingLevel) }
                },
            )
        }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.common_action_save
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.*
import org.orev.nahidka.ui.common.dialog.MutationDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.tasks.component.TaskRatingReactions
import org.orev.nahidka.ui.tasks.component.title
import org.orev.nahidka.ui.tasks.model.TaskDraft
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

@Composable
internal fun TaskEditorDialog(
    taskEditor: MutationDialogController<TaskDraft>,
    ratingLevels: List<TaskRatingLevel>,
) {
    MutationDialog(
        dialogController = taskEditor,
        title = { taskDraft ->
            stringResource(
                if (taskDraft.editedTask == null) Res.string.tasks_creation_title else Res.string.tasks_editing_title,
            )
        },
        confirmationTitle = stringResource(CommonResources.string.common_action_save),
    ) { taskDraft ->
        TaskForm(taskDraft, ratingLevels, taskEditor::edit)
    }
}

@Composable
private fun TaskForm(
    taskDraft: TaskDraft,
    ratingLevels: List<TaskRatingLevel>,
    onTaskDraftEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    TitleField(taskDraft.title) { title -> onTaskDraftEdit { draft -> draft.copy(title = title) } }
    DescriptionField(taskDraft.description) { description ->
        onTaskDraftEdit { draft -> draft.copy(description = description) }
    }
    ChoiceField(
        title = stringResource(Res.string.tasks_field_status),
        options = TaskStatus.entries,
        selectedOption = taskDraft.status,
        optionTitle = { status -> stringResource(status.title) },
        onOptionSelect = { status -> onTaskDraftEdit { draft -> draft.withStatus(status) } },
    )
    DateField(
        date = taskDraft.dueDate,
        title = stringResource(Res.string.tasks_field_due_date),
        onDateSelect = { dueDate -> onTaskDraftEdit { draft -> draft.copy(dueDate = dueDate) } },
        onDateClear = { onTaskDraftEdit { draft -> draft.copy(dueDate = null) } },
    )
    if (taskDraft.status.acceptsRating && ratingLevels.isNotEmpty()) {
        LabeledField(stringResource(Res.string.tasks_field_rating)) {
            TaskRatingReactions(
                ratingLevels = ratingLevels,
                selectedRatingLevel = taskDraft.ratingLevel,
                onRatingLevelSelect = { ratingLevel ->
                    onTaskDraftEdit { draft -> draft.copy(ratingLevel = ratingLevel) }
                },
            )
        }
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskDeletionDialog.kt` — modified

`DeletionDialog` with the task-specific title and message.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_action_delete
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_message
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskDeletionDialog(taskDeletion: TaskDialogController<TaskItem>) {
    TaskDialog(
        dialogController = taskDeletion,
        title = { stringResource(Res.string.tasks_deletion_title) },
        confirmationTitle = stringResource(Res.string.tasks_action_delete),
    ) { taskItem ->
        Text(stringResource(Res.string.tasks_deletion_message, taskItem.task.title))
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_message
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.dialog.DeletionDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskDeletionDialog(taskDeletion: MutationDialogController<TaskItem>) {
    DeletionDialog(taskDeletion, stringResource(Res.string.tasks_deletion_title)) { taskItem ->
        stringResource(Res.string.tasks_deletion_message, taskItem.task.title)
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskRatingLevelsDialog.kt` — modified

`MutationDialog` and the shared Save / Delete labels.

_Change 1 of 2 (old line 7 / new line 7)_

**Old:**

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.tasks.model.TaskRatingLevelsDraft

@Composable
internal fun TaskRatingLevelsDialog(
    ratingLevelsEditor: TaskDialogController<TaskRatingLevelsDraft>,
    onRatingLevelAdd: () -> Unit,
) {
    TaskDialog(
        dialogController = ratingLevelsEditor,
        title = { stringResource(Res.string.tasks_rating_levels) },
        confirmationTitle = stringResource(Res.string.tasks_action_save),
    ) { ratingLevelsDraft ->
        Text(
```

**New:**

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nahidka.shared.ui.common.generated.resources.common_action_delete
import nahidka.shared.ui.common.generated.resources.common_action_save
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.dialog.MutationDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.tasks.model.TaskRatingLevelsDraft
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

@Composable
internal fun TaskRatingLevelsDialog(
    ratingLevelsEditor: MutationDialogController<TaskRatingLevelsDraft>,
    onRatingLevelAdd: () -> Unit,
) {
    MutationDialog(
        dialogController = ratingLevelsEditor,
        title = { stringResource(Res.string.tasks_rating_levels) },
        confirmationTitle = stringResource(CommonResources.string.common_action_save),
    ) { ratingLevelsDraft ->
        Text(
```

_Change 2 of 2 (old line 71 / new line 76)_

**Old:**

```kotlin
    }
    add(
        MenuAction(stringResource(Res.string.tasks_action_delete)) {
            onRatingLevelsDraftEdit { draft -> draft.removing(ratingLevelIndex) }
        },
```

**New:**

```kotlin
    }
    add(
        MenuAction(stringResource(CommonResources.string.common_action_delete)) {
            onRatingLevelsDraftEdit { draft -> draft.removing(ratingLevelIndex) }
        },
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskDueDateText.kt` — deleted

Moved to `ui/common` as `DateText`.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import org.orev.nahidka.ui.common.format.DAY_FORMAT

@Composable
internal fun TaskDueDateText(dueDate: LocalDate, modifier: Modifier = Modifier) {
    Text(
        text = "📅 ${dueDate.format(DAY_FORMAT)}",
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}
```

**New:** _(file deleted — moved unchanged to `shared/ui/common/.../component/DateText.kt`, see Step 2)_

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskRatingReactions.kt` — modified

It now only adapts rating levels to `ChoiceChips`. Its two callers are unchanged.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel

@Composable
internal fun TaskRatingReactions(
    ratingLevels: List<TaskRatingLevel>,
    selectedRatingLevel: TaskRatingLevel?,
    onRatingLevelSelect: (TaskRatingLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ratingLevels.forEach { ratingLevel ->
            val selected = ratingLevel.identifier == selectedRatingLevel?.identifier

            FilterChip(
                selected = selected,
                onClick = { onRatingLevelSelect(ratingLevel.takeUnless { selected }) },
                label = { Text(ratingLevel.reaction, style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.ChoiceChips

@Composable
internal fun TaskRatingReactions(
    ratingLevels: List<TaskRatingLevel>,
    selectedRatingLevel: TaskRatingLevel?,
    onRatingLevelSelect: (TaskRatingLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceChips(
        options = ratingLevels,
        selectedOption = selectedRatingLevel,
        optionTitle = { ratingLevel -> ratingLevel.reaction },
        onOptionSelect = onRatingLevelSelect,
        modifier = modifier,
    )
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskMenuActions.kt` — modified

`editingMenuActions`.

_Change 1 of 1 (old line 6 / new line 6)_

**Old:**

```kotlin
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun taskEditingActions(taskItem: TaskItem, taskInteractions: TaskInteractions): List<MenuAction> = listOf(
    MenuAction(stringResource(Res.string.tasks_action_edit)) { taskInteractions.onTaskEdit(taskItem) },
    MenuAction(stringResource(Res.string.tasks_action_delete)) { taskInteractions.onTaskDelete(taskItem) },
)

@Composable
```

**New:**

```kotlin
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.editingMenuActions
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun taskEditingActions(taskItem: TaskItem, taskInteractions: TaskInteractions): List<MenuAction> =
    editingMenuActions(
        onEdit = { taskInteractions.onTaskEdit(taskItem) },
        onDelete = { taskInteractions.onTaskDelete(taskItem) },
    )

@Composable
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskCard.kt` — modified

`DateText`.

_Change 1 of 2 (old line 11 / new line 11)_

**Old:**

```kotlin
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.tasks.component.TaskDueDateText
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.taskEditingActions
```

**New:**

```kotlin
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.taskEditingActions
```

_Change 2 of 2 (old line 62 / new line 62)_

**Old:**

```kotlin
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                    Spacer(Modifier.weight(1f))
```

**New:**

```kotlin
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    taskItem.task.dueDate?.let { dueDate ->
                        DateText(dueDate)
                    }
                    Spacer(Modifier.weight(1f))
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskCardList.kt` — modified

It now only adapts task items to `CardList`. Its two callers in `TasksBoard` are unchanged.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_column_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskCardList(
    taskItems: List<TaskItem>,
    modifier: Modifier = Modifier,
    taskCard: @Composable (TaskItem) -> Unit,
) {
    if (taskItems.isEmpty()) {
        Text(
            text = stringResource(Res.string.tasks_column_empty),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(taskItems, key = { taskItem -> taskItem.task.identifier }) { taskItem ->
                taskCard(taskItem)
            }
        }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_column_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.CardList
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskCardList(
    taskItems: List<TaskItem>,
    modifier: Modifier = Modifier,
    taskCard: @Composable (TaskItem) -> Unit,
) {
    CardList(
        listItems = taskItems,
        itemKey = { taskItem -> taskItem.task.identifier },
        emptyListMessage = stringResource(Res.string.tasks_column_empty),
        modifier = modifier,
        itemCard = taskCard,
    )
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/list/TaskListRow.kt` — modified

`DateText`.

_Change 1 of 3 (old line 11 / new line 11)_

**Old:**

```kotlin
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.component.TaskDueDateText
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.TaskStatusMenu
```

**New:**

```kotlin
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.TaskStatusMenu
```

_Change 2 of 3 (old line 52 / new line 52)_

**Old:**

```kotlin
                    TaskStatusMenu(taskItem, taskInteractions)
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
```

**New:**

```kotlin
                    TaskStatusMenu(taskItem, taskInteractions)
                    taskItem.task.dueDate?.let { dueDate ->
                        DateText(dueDate)
                    }
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
```

_Change 3 of 3 (old line 66 / new line 66)_

**Old:**

```kotlin
                Box(Modifier.weight(TaskListColumn.DUE_DATE.weight)) {
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                }
```

**New:**

```kotlin
                Box(Modifier.weight(TaskListColumn.DUE_DATE.weight)) {
                    taskItem.task.dueDate?.let { dueDate ->
                        DateText(dueDate)
                    }
                }
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/list/TaskListColumn.kt` — modified

The shared Title / Description labels.

**Old:**

```kotlin
package org.orev.nahidka.ui.tasks.list

import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.StringResource

internal enum class TaskListColumn(val title: StringResource, val weight: Float) {
    TITLE(Res.string.tasks_field_title, 2f),
    DESCRIPTION(Res.string.tasks_field_description, 3f),
    STATUS(Res.string.tasks_field_status, 1.4f),
    DUE_DATE(Res.string.tasks_field_due_date, 1.2f),
    RATING(Res.string.tasks_field_rating, 1.4f),
}
```

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.list

import nahidka.shared.ui.common.generated.resources.common_field_description
import nahidka.shared.ui.common.generated.resources.common_field_title
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

internal enum class TaskListColumn(val title: StringResource, val weight: Float) {
    TITLE(CommonResources.string.common_field_title, 2f),
    DESCRIPTION(CommonResources.string.common_field_description, 3f),
    STATUS(Res.string.tasks_field_status, 1.4f),
    DUE_DATE(Res.string.tasks_field_due_date, 1.2f),
    RATING(Res.string.tasks_field_rating, 1.4f),
}
```

#### `shared/ui/tasks/src/commonMain/composeResources/values/strings.xml` — modified

Removes the six strings now in `ui/common`.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Tasks</string>
    <string name="tasks_view_board">Board</string>
    <string name="tasks_view_list">List</string>
    <string name="tasks_create">Create new task</string>
    <string name="tasks_creation_title">New task</string>
    <string name="tasks_editing_title">Edit task</string>
    <string name="tasks_action_save">Save</string>
    <string name="tasks_action_edit">Edit</string>
    <string name="tasks_action_delete">Delete</string>
    <string name="tasks_action_move_to">Move to %1$s</string>
    <string name="tasks_action_move_up">Move up</string>
    <string name="tasks_action_move_down">Move down</string>
    <string name="tasks_field_title">Title</string>
    <string name="tasks_field_description">Description</string>
    <string name="tasks_field_status">Status</string>
    <string name="tasks_field_due_date">Due date</string>
    <string name="tasks_field_rating">Rating</string>
    <string name="tasks_status_to_do">To Do</string>
    <string name="tasks_status_in_progress">In Progress</string>
    <string name="tasks_status_done">Done</string>
    <string name="tasks_status_count">%1$s (%2$d)</string>
    <string name="tasks_column_empty">No tasks</string>
    <string name="tasks_list_empty">There are no tasks yet</string>
    <string name="tasks_rating_rate">Rate</string>
    <string name="tasks_rating_levels">Rating reactions</string>
    <string name="tasks_rating_levels_hint">Reactions go from the worst to the best. Removing a reaction also removes it from rated tasks.</string>
    <string name="tasks_rating_level_title">Reaction %1$d</string>
    <string name="tasks_rating_level_add">＋ Add reaction</string>
    <string name="tasks_deletion_title">Delete task?</string>
    <string name="tasks_deletion_message">“%1$s” will be deleted.</string>
    <string name="tasks_error_unsaved">This change could not be saved</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Tasks</string>
    <string name="tasks_view_board">Board</string>
    <string name="tasks_view_list">List</string>
    <string name="tasks_create">Create new task</string>
    <string name="tasks_creation_title">New task</string>
    <string name="tasks_editing_title">Edit task</string>
    <string name="tasks_action_move_to">Move to %1$s</string>
    <string name="tasks_action_move_up">Move up</string>
    <string name="tasks_action_move_down">Move down</string>
    <string name="tasks_field_status">Status</string>
    <string name="tasks_field_due_date">Due date</string>
    <string name="tasks_field_rating">Rating</string>
    <string name="tasks_status_to_do">To Do</string>
    <string name="tasks_status_in_progress">In Progress</string>
    <string name="tasks_status_done">Done</string>
    <string name="tasks_status_count">%1$s (%2$d)</string>
    <string name="tasks_column_empty">No tasks</string>
    <string name="tasks_list_empty">There are no tasks yet</string>
    <string name="tasks_rating_rate">Rate</string>
    <string name="tasks_rating_levels">Rating reactions</string>
    <string name="tasks_rating_levels_hint">Reactions go from the worst to the best. Removing a reaction also removes it from rated tasks.</string>
    <string name="tasks_rating_level_title">Reaction %1$d</string>
    <string name="tasks_rating_level_add">＋ Add reaction</string>
    <string name="tasks_deletion_title">Delete task?</string>
    <string name="tasks_deletion_message">“%1$s” will be deleted.</string>
</resources>
```

#### `shared/ui/tasks/src/commonMain/composeResources/values-uk/strings.xml` — modified

Same removal (Ukrainian).

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Завдання</string>
    <string name="tasks_view_board">Дошка</string>
    <string name="tasks_view_list">Список</string>
    <string name="tasks_create">Створити завдання</string>
    <string name="tasks_creation_title">Нове завдання</string>
    <string name="tasks_editing_title">Редагування завдання</string>
    <string name="tasks_action_save">Зберегти</string>
    <string name="tasks_action_edit">Редагувати</string>
    <string name="tasks_action_delete">Видалити</string>
    <string name="tasks_action_move_to">Перемістити в «%1$s»</string>
    <string name="tasks_action_move_up">Перемістити вгору</string>
    <string name="tasks_action_move_down">Перемістити вниз</string>
    <string name="tasks_field_title">Назва</string>
    <string name="tasks_field_description">Опис</string>
    <string name="tasks_field_status">Статус</string>
    <string name="tasks_field_due_date">Термін</string>
    <string name="tasks_field_rating">Оцінка</string>
    <string name="tasks_status_to_do">До виконання</string>
    <string name="tasks_status_in_progress">В роботі</string>
    <string name="tasks_status_done">Готово</string>
    <string name="tasks_status_count">%1$s (%2$d)</string>
    <string name="tasks_column_empty">Немає завдань</string>
    <string name="tasks_list_empty">Завдань ще немає</string>
    <string name="tasks_rating_rate">Оцінити</string>
    <string name="tasks_rating_levels">Реакції для оцінки</string>
    <string name="tasks_rating_levels_hint">Реакції йдуть від найгіршої до найкращої. Видалення реакції прибирає її й з оцінених завдань.</string>
    <string name="tasks_rating_level_title">Реакція %1$d</string>
    <string name="tasks_rating_level_add">＋ Додати реакцію</string>
    <string name="tasks_deletion_title">Видалити завдання?</string>
    <string name="tasks_deletion_message">Завдання «%1$s» буде видалено.</string>
    <string name="tasks_error_unsaved">Не вдалося зберегти зміни</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Завдання</string>
    <string name="tasks_view_board">Дошка</string>
    <string name="tasks_view_list">Список</string>
    <string name="tasks_create">Створити завдання</string>
    <string name="tasks_creation_title">Нове завдання</string>
    <string name="tasks_editing_title">Редагування завдання</string>
    <string name="tasks_action_move_to">Перемістити в «%1$s»</string>
    <string name="tasks_action_move_up">Перемістити вгору</string>
    <string name="tasks_action_move_down">Перемістити вниз</string>
    <string name="tasks_field_status">Статус</string>
    <string name="tasks_field_due_date">Термін</string>
    <string name="tasks_field_rating">Оцінка</string>
    <string name="tasks_status_to_do">До виконання</string>
    <string name="tasks_status_in_progress">В роботі</string>
    <string name="tasks_status_done">Готово</string>
    <string name="tasks_status_count">%1$s (%2$d)</string>
    <string name="tasks_column_empty">Немає завдань</string>
    <string name="tasks_list_empty">Завдань ще немає</string>
    <string name="tasks_rating_rate">Оцінити</string>
    <string name="tasks_rating_levels">Реакції для оцінки</string>
    <string name="tasks_rating_levels_hint">Реакції йдуть від найгіршої до найкращої. Видалення реакції прибирає її й з оцінених завдань.</string>
    <string name="tasks_rating_level_title">Реакція %1$d</string>
    <string name="tasks_rating_level_add">＋ Додати реакцію</string>
    <string name="tasks_deletion_title">Видалити завдання?</string>
    <string name="tasks_deletion_message">Завдання «%1$s» буде видалено.</string>
</resources>
```

#### `shared/ui/tasks/src/commonMain/composeResources/values-ru/strings.xml` — modified

Same removal (Russian).

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Задачи</string>
    <string name="tasks_view_board">Доска</string>
    <string name="tasks_view_list">Список</string>
    <string name="tasks_create">Создать задачу</string>
    <string name="tasks_creation_title">Новая задача</string>
    <string name="tasks_editing_title">Редактирование задачи</string>
    <string name="tasks_action_save">Сохранить</string>
    <string name="tasks_action_edit">Редактировать</string>
    <string name="tasks_action_delete">Удалить</string>
    <string name="tasks_action_move_to">Переместить в «%1$s»</string>
    <string name="tasks_action_move_up">Переместить вверх</string>
    <string name="tasks_action_move_down">Переместить вниз</string>
    <string name="tasks_field_title">Название</string>
    <string name="tasks_field_description">Описание</string>
    <string name="tasks_field_status">Статус</string>
    <string name="tasks_field_due_date">Срок</string>
    <string name="tasks_field_rating">Оценка</string>
    <string name="tasks_status_to_do">К выполнению</string>
    <string name="tasks_status_in_progress">В работе</string>
    <string name="tasks_status_done">Готово</string>
    <string name="tasks_status_count">%1$s (%2$d)</string>
    <string name="tasks_column_empty">Нет задач</string>
    <string name="tasks_list_empty">Задач пока нет</string>
    <string name="tasks_rating_rate">Оценить</string>
    <string name="tasks_rating_levels">Реакции для оценки</string>
    <string name="tasks_rating_levels_hint">Реакции идут от худшей к лучшей. Удаление реакции убирает её и из оценённых задач.</string>
    <string name="tasks_rating_level_title">Реакция %1$d</string>
    <string name="tasks_rating_level_add">＋ Добавить реакцию</string>
    <string name="tasks_deletion_title">Удалить задачу?</string>
    <string name="tasks_deletion_message">Задача «%1$s» будет удалена.</string>
    <string name="tasks_error_unsaved">Не удалось сохранить изменения</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Задачи</string>
    <string name="tasks_view_board">Доска</string>
    <string name="tasks_view_list">Список</string>
    <string name="tasks_create">Создать задачу</string>
    <string name="tasks_creation_title">Новая задача</string>
    <string name="tasks_editing_title">Редактирование задачи</string>
    <string name="tasks_action_move_to">Переместить в «%1$s»</string>
    <string name="tasks_action_move_up">Переместить вверх</string>
    <string name="tasks_action_move_down">Переместить вниз</string>
    <string name="tasks_field_status">Статус</string>
    <string name="tasks_field_due_date">Срок</string>
    <string name="tasks_field_rating">Оценка</string>
    <string name="tasks_status_to_do">К выполнению</string>
    <string name="tasks_status_in_progress">В работе</string>
    <string name="tasks_status_done">Готово</string>
    <string name="tasks_status_count">%1$s (%2$d)</string>
    <string name="tasks_column_empty">Нет задач</string>
    <string name="tasks_list_empty">Задач пока нет</string>
    <string name="tasks_rating_rate">Оценить</string>
    <string name="tasks_rating_levels">Реакции для оценки</string>
    <string name="tasks_rating_levels_hint">Реакции идут от худшей к лучшей. Удаление реакции убирает её и из оценённых задач.</string>
    <string name="tasks_rating_level_title">Реакция %1$d</string>
    <string name="tasks_rating_level_add">＋ Добавить реакцию</string>
    <string name="tasks_deletion_title">Удалить задачу?</string>
    <string name="tasks_deletion_message">Задача «%1$s» будет удалена.</string>
</resources>
```

### Step 4 — Goals UI: build, model, ViewModel, DI

Same structure as Tasks: a session-retained ViewModel, a draft for the editor, and a Metro graph used by both the host and the tests.

#### `shared/ui/goal/build.gradle.kts` — modified

Metro, `ui:common`, lifecycle and the JVM UI-test dependencies, matching `shared/ui/tasks/build.gradle.kts`.

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
            api(project(":shared:core:feature:goals"))
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
            api(project(":shared:core:feature:goals"))
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

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/model/GoalDraft.kt` — new

Editor state. It builds creation and update requests; `nullablePatch` (`core:common`) turns picture and deadline edits into keep / set / clear. Save is enabled once the title isn't blank.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.model

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.nullablePatch
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest

data class GoalDraft(
    val editedGoal: GoalRecord?,
    val title: String,
    val description: String,
    val picture: GoalPicture?,
    val progressPercentage: Float,
    val deadlineDate: LocalDate?,
) {

    val submittable: Boolean
        get() = title.isNotBlank()

    fun toCreationRequest(identifier: String): GoalCreationRequest = GoalCreationRequest(
        identifier = identifier,
        title = title.trim(),
        description = description.trim(),
        picture = picture,
        progressPercentage = progressPercentage,
        deadlineDate = deadlineDate,
    )

    fun toUpdateRequest(editedGoal: GoalRecord): GoalUpdateRequest = GoalUpdateRequest(
        identifier = editedGoal.identifier,
        title = title.trim(),
        description = description.trim(),
        picturePatch = nullablePatch(editedGoal.picture, picture),
        progressPercentage = progressPercentage,
        deadlinePatch = nullablePatch(editedGoal.deadlineDate, deadlineDate),
    )

    companion object {

        fun creation(): GoalDraft = GoalDraft(
            editedGoal = null,
            title = "",
            description = "",
            picture = null,
            progressPercentage = 0f,
            deadlineDate = null,
        )

        fun editing(goal: GoalRecord): GoalDraft = GoalDraft(
            editedGoal = goal,
            title = goal.title,
            description = goal.description,
            picture = goal.picture,
            progressPercentage = goal.progressPercentage,
            deadlineDate = goal.deadlineDate,
        )
    }
}
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/GoalsViewModel.kt` — new

Exposes the core `StateFlow` as is: no mapping and no second copy of the state. Two dialog controllers handle editing and deletion.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.goals.service.GoalsRepository
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.goal.model.GoalDraft

@Inject
class GoalsViewModel(
    goalsRepository: GoalsRepository,
    private val goalsManager: GoalsManager,
    private val identifierGenerator: IdentifierGenerator,
) : ViewModel() {

    val goalsState: StateFlow<GoalsSnapshot> = goalsRepository.goalsState

    val goalEditor = MutationDialogController<GoalDraft>(viewModelScope, GoalDraft::submittable) { goalDraft ->
        saveGoal(goalDraft)
    }

    val goalDeletion = MutationDialogController<GoalRecord>(viewModelScope) { goal ->
        goalsManager.deleteGoal(goal.identifier)
    }

    fun openGoalCreation() {
        goalEditor.open(GoalDraft.creation())
    }

    fun openGoalEditing(goal: GoalRecord) {
        goalEditor.open(GoalDraft.editing(goal))
    }

    private suspend fun saveGoal(goalDraft: GoalDraft) {
        val editedGoal = goalDraft.editedGoal

        if (editedGoal == null) {
            goalsManager.createGoal(goalDraft.toCreationRequest(identifierGenerator.next()))
        } else {
            goalsManager.updateGoal(goalDraft.toUpdateRequest(editedGoal))
        }
    }
}
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/GoalsScreenGraph.kt` — new

The DI composition root for the host and the tests.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.core.common.IdentifierGeneratorBindings
import org.orev.nahidka.feature.goals.di.GoalsBindings
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.service.GoalsManager

@DependencyGraph(GoalsSessionScope::class, bindingContainers = [GoalsBindings::class, IdentifierGeneratorBindings::class])
interface GoalsScreenGraph {
    val goalsViewModel: GoalsViewModel
    val goalsManager: GoalsManager
}
```

### Step 5 — String resources (en / uk / ru)

`feature_goal_title` is kept. New keys follow the `<module>_*` convention. The mockup's “Compleated” is spelled “Completed”. A single `%` is used deliberately: Compose resources substitute only `%1$d`/`%1$s` and would print `%%` as is.

#### `shared/ui/goal/src/commonMain/composeResources/values/strings.xml` — modified

English.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_goal_title">Goals</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_goal_title">Goals</string>
    <string name="goal_create">Create goal</string>
    <string name="goal_creation_title">New goal</string>
    <string name="goal_editing_title">Edit goal</string>
    <string name="goal_field_picture">Picture</string>
    <string name="goal_field_progress">Progress: %1$d%</string>
    <string name="goal_field_deadline">Deadline</string>
    <string name="goal_progress_completed">Completed on %1$d%</string>
    <string name="goal_action_remove_picture">Remove picture</string>
    <string name="goal_list_empty">There are no goals yet</string>
    <string name="goal_deletion_title">Delete goal?</string>
    <string name="goal_deletion_message">“%1$s” will be deleted.</string>
</resources>
```

#### `shared/ui/goal/src/commonMain/composeResources/values-uk/strings.xml` — modified

Ukrainian.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_goal_title">Цілі</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_goal_title">Цілі</string>
    <string name="goal_create">Створити ціль</string>
    <string name="goal_creation_title">Нова ціль</string>
    <string name="goal_editing_title">Редагування цілі</string>
    <string name="goal_field_picture">Зображення</string>
    <string name="goal_field_progress">Прогрес: %1$d%</string>
    <string name="goal_field_deadline">Термін</string>
    <string name="goal_progress_completed">Виконано на %1$d%</string>
    <string name="goal_action_remove_picture">Прибрати зображення</string>
    <string name="goal_list_empty">Цілей ще немає</string>
    <string name="goal_deletion_title">Видалити ціль?</string>
    <string name="goal_deletion_message">Ціль «%1$s» буде видалено.</string>
</resources>
```

#### `shared/ui/goal/src/commonMain/composeResources/values-ru/strings.xml` — modified

Russian.

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_goal_title">Цели</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_goal_title">Цели</string>
    <string name="goal_create">Создать цель</string>
    <string name="goal_creation_title">Новая цель</string>
    <string name="goal_editing_title">Редактирование цели</string>
    <string name="goal_field_picture">Изображение</string>
    <string name="goal_field_progress">Прогресс: %1$d%</string>
    <string name="goal_field_deadline">Срок</string>
    <string name="goal_progress_completed">Выполнено на %1$d%</string>
    <string name="goal_action_remove_picture">Убрать изображение</string>
    <string name="goal_list_empty">Целей пока нет</string>
    <string name="goal_deletion_title">Удалить цель?</string>
    <string name="goal_deletion_message">Цель «%1$s» будет удалена.</string>
</resources>
```

### Step 6 — Picture and card

The two visual building blocks of the mockup row.

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/component/GoalEmojiPictures.kt` — new

The 12 emoji offered in the editor. 🧳 replaces ✈️, which Skia's font fallback draws in black and white on desktop and web.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.component

import org.orev.nahidka.feature.goals.dto.GoalPicture

internal val GOAL_EMOJI_PICTURES: List<GoalPicture.Emoji> =
    listOf("🎯", "🏃", "📚", "💪", "💰", "🧳", "🏠", "🎓", "🎨", "🎸", "🧘", "🌱")
        .map(GoalPicture::Emoji)
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/component/GoalPictureView.kt` — new

The mockup's “ICON or PHOTO” box: an emoji scaled to the box, a cropped photo, or the title's first letter when there is no picture. The same box appears on cards and in the editor.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.ui.common.photo.rememberPhotoBitmap

private const val GOAL_PICTURE_SYMBOL_SCALE = 0.5f

@Composable
internal fun GoalPictureView(picture: GoalPicture?, goalTitle: String, pictureSize: Dp) {
    Box(
        modifier = Modifier
            .size(pictureSize)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when (picture) {
            is GoalPicture.Emoji -> GoalPictureSymbol(picture.symbol, pictureSize)
            is GoalPicture.Photo -> GoalPicturePhoto(picture, goalTitle)
            null -> GoalPictureSymbol(
                goalTitle
                    .take(1)
                    .uppercase(),
                pictureSize,
            )
        }
    }
}

@Composable
private fun GoalPictureSymbol(symbol: String, pictureSize: Dp) {
    Text(
        text = symbol,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        fontSize = with(LocalDensity.current) { (pictureSize * GOAL_PICTURE_SYMBOL_SCALE).toSp() },
        maxLines = 1,
    )
}

@Composable
private fun GoalPicturePhoto(photo: GoalPicture.Photo, goalTitle: String) {
    rememberPhotoBitmap(photo.content)?.let { photoBitmap ->
        Image(
            bitmap = photoBitmap,
            contentDescription = goalTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/card/GoalCard.kt` — new

The mockup row: picture, title, “Completed on N%” (primary color at 100 %), ⋮, description (3 lines), progress bar and optional deadline. Clicking the card opens the editor. Compact layouts use a 64 dp picture and move the percentage under the title.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.card

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_progress_completed
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.component.editingMenuActions
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.goal.component.GoalPictureView
import kotlin.math.roundToInt

private const val GOAL_CARD_DESCRIPTION_MAXIMUM_LINES = 3

@Composable
internal fun GoalCard(
    goal: GoalRecord,
    layoutWidth: LayoutWidth,
    onGoalEdit: (GoalRecord) -> Unit,
    onGoalDelete: (GoalRecord) -> Unit,
) {
    OutlinedCard(
        onClick = { onGoalEdit(goal) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GoalPictureView(goal.picture, goal.title, layoutWidth.goalPictureSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (layoutWidth == LayoutWidth.EXPANDED) {
                        GoalProgressText(goal)
                    }
                    MoreActionsMenu(
                        editingMenuActions(
                            onEdit = { onGoalEdit(goal) },
                            onDelete = { onGoalDelete(goal) },
                        ),
                    )
                }
                Column(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (layoutWidth == LayoutWidth.COMPACT) {
                        GoalProgressText(goal)
                    }
                    if (goal.description.isNotBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = GOAL_CARD_DESCRIPTION_MAXIMUM_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { goal.progressPercentage / GoalRecord.PROGRESS_PERCENTAGE_RANGE.endInclusive },
                            modifier = Modifier.weight(1f),
                        )
                        goal.deadlineDate?.let { deadlineDate ->
                            DateText(deadlineDate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressText(goal: GoalRecord) {
    Text(
        text = stringResource(Res.string.goal_progress_completed, goal.progressPercentage.roundToInt()),
        style = MaterialTheme.typography.labelLarge,
        color = if (goal.progressPercentage == GoalRecord.PROGRESS_PERCENTAGE_RANGE.endInclusive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
    )
}

private val LayoutWidth.goalPictureSize: Dp
    get() = when (this) {
        LayoutWidth.COMPACT -> 64.dp
        LayoutWidth.EXPANDED -> 112.dp
    }
```

### Step 7 — Dialogs

Both dialogs are thin: `MutationDialog` and `DeletionDialog` from `ui/common` provide the buttons, the error text and the submission handling.

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/dialog/GoalPictureField.kt` — new

Preview, **Choose photo**, **Remove picture**, and the emoji chips. Tapping the selected emoji clears it.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.common.generated.resources.common_action_choose_photo
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_action_remove_picture
import nahidka.shared.ui.goal.generated.resources.goal_field_picture
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.ui.common.component.ChoiceChips
import org.orev.nahidka.ui.common.component.LabeledField
import org.orev.nahidka.ui.common.photo.rememberPhotoPicker
import org.orev.nahidka.ui.goal.component.GOAL_EMOJI_PICTURES
import org.orev.nahidka.ui.goal.component.GoalPictureView
import org.orev.nahidka.ui.goal.model.GoalDraft
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

private val GOAL_EDITOR_PICTURE_SIZE = 72.dp

@Composable
internal fun GoalPictureField(goalDraft: GoalDraft, onGoalDraftEdit: ((GoalDraft) -> GoalDraft) -> Unit) {
    val openPhotoPicker = rememberPhotoPicker { photoContent ->
        onGoalDraftEdit { draft -> draft.copy(picture = GoalPicture.Photo(photoContent)) }
    }

    LabeledField(stringResource(Res.string.goal_field_picture)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoalPictureView(goalDraft.picture, goalDraft.title, GOAL_EDITOR_PICTURE_SIZE)
            Column {
                TextButton(onClick = openPhotoPicker) {
                    Text(stringResource(CommonResources.string.common_action_choose_photo))
                }
                if (goalDraft.picture != null) {
                    TextButton(onClick = { onGoalDraftEdit { draft -> draft.copy(picture = null) } }) {
                        Text(stringResource(Res.string.goal_action_remove_picture))
                    }
                }
            }
        }
        ChoiceChips(
            options = GOAL_EMOJI_PICTURES,
            selectedOption = goalDraft.picture as? GoalPicture.Emoji,
            optionTitle = { emojiPicture -> emojiPicture.symbol },
            onOptionSelect = { emojiPicture -> onGoalDraftEdit { draft -> draft.copy(picture = emojiPicture) } },
        )
    }
}
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/dialog/GoalEditorDialog.kt` — new

Title, description, picture, progress slider (0–100 %, 5 % steps, live “Progress: N%” label) and deadline. The content scrolls on small screens.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.dialog

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.common_action_save
import nahidka.shared.ui.goal.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.DateField
import org.orev.nahidka.ui.common.component.DescriptionField
import org.orev.nahidka.ui.common.component.LabeledField
import org.orev.nahidka.ui.common.component.TitleField
import org.orev.nahidka.ui.common.dialog.MutationDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.goal.model.GoalDraft
import kotlin.math.roundToInt
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

private const val GOAL_PROGRESS_STEP_PERCENTAGE = 5f
private val GOAL_PROGRESS_SLIDER_STEPS =
    (GoalRecord.PROGRESS_PERCENTAGE_RANGE.endInclusive / GOAL_PROGRESS_STEP_PERCENTAGE).roundToInt() - 1

@Composable
internal fun GoalEditorDialog(goalEditor: MutationDialogController<GoalDraft>) {
    MutationDialog(
        dialogController = goalEditor,
        title = { goalDraft ->
            stringResource(
                if (goalDraft.editedGoal == null) Res.string.goal_creation_title else Res.string.goal_editing_title,
            )
        },
        confirmationTitle = stringResource(CommonResources.string.common_action_save),
    ) { goalDraft ->
        GoalForm(goalDraft, goalEditor::edit)
    }
}

@Composable
private fun GoalForm(goalDraft: GoalDraft, onGoalDraftEdit: ((GoalDraft) -> GoalDraft) -> Unit) {
    TitleField(goalDraft.title) { title -> onGoalDraftEdit { draft -> draft.copy(title = title) } }
    DescriptionField(goalDraft.description) { description ->
        onGoalDraftEdit { draft -> draft.copy(description = description) }
    }
    GoalPictureField(goalDraft, onGoalDraftEdit)
    LabeledField(stringResource(Res.string.goal_field_progress, goalDraft.progressPercentage.roundToInt())) {
        Slider(
            value = goalDraft.progressPercentage,
            onValueChange = { progressPercentage ->
                onGoalDraftEdit { draft -> draft.copy(progressPercentage = progressPercentage) }
            },
            valueRange = GoalRecord.PROGRESS_PERCENTAGE_RANGE,
            steps = GOAL_PROGRESS_SLIDER_STEPS,
        )
    }
    DateField(
        date = goalDraft.deadlineDate,
        title = stringResource(Res.string.goal_field_deadline),
        onDateSelect = { deadlineDate -> onGoalDraftEdit { draft -> draft.copy(deadlineDate = deadlineDate) } },
        onDateClear = { onGoalDraftEdit { draft -> draft.copy(deadlineDate = null) } },
    )
}
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/dialog/GoalDeletionDialog.kt` — new

Confirmation with the goal's name.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_deletion_message
import nahidka.shared.ui.goal.generated.resources.goal_deletion_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.dialog.DeletionDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController

@Composable
internal fun GoalDeletionDialog(goalDeletion: MutationDialogController<GoalRecord>) {
    DeletionDialog(goalDeletion, stringResource(Res.string.goal_deletion_title)) { goal ->
        stringResource(Res.string.goal_deletion_message, goal.title)
    }
}
```

### Step 8 — Screen entry point, demo data, README

The public entry point replaces the old table.

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/GoalsScreen.kt` — new

Measures its own width (`BoxWithConstraints`), so a narrow desktop window behaves like a phone. **Create goal** is right-aligned, as in the mockup, and the cards come below.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_create
import nahidka.shared.ui.goal.generated.resources.goal_list_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.AddButton
import org.orev.nahidka.ui.common.component.CardList
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.goal.card.GoalCard
import org.orev.nahidka.ui.goal.dialog.GoalDeletionDialog
import org.orev.nahidka.ui.goal.dialog.GoalEditorDialog

@Composable
fun GoalsScreen(goalsViewModel: GoalsViewModel, modifier: Modifier = Modifier) {
    val goalsSnapshot by goalsViewModel.goalsState.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val layoutWidth = LayoutWidth.of(maxWidth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(layoutWidth.screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AddButton(stringResource(Res.string.goal_create), layoutWidth, goalsViewModel::openGoalCreation)
            }
            CardList(
                listItems = goalsSnapshot.goals,
                itemKey = GoalRecord::identifier,
                emptyListMessage = stringResource(Res.string.goal_list_empty),
            ) { goal ->
                GoalCard(goal, layoutWidth, goalsViewModel::openGoalEditing, goalsViewModel.goalDeletion::open)
            }
        }
    }

    GoalEditorDialog(goalsViewModel.goalEditor)
    GoalDeletionDialog(goalsViewModel.goalDeletion)
}
```

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/GoalsTable.kt` — deleted

The old prototype table, which used tap to add 10 %.

**Old:**

```kotlin
package org.orev.nahidka.ui.goal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.GoalRecord

@Composable
fun GoalsTable(
    goals: List<GoalRecord>,
    onGoalClick: (GoalRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(goals, key = GoalRecord::identifier) { goal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoalClick(goal) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(goal.title, Modifier.weight(1f))
                Text("${goal.progressPercentage.toInt()}%")
                goal.deadlineInstant?.let { deadline -> Text(deadline.toString()) }
            }
            HorizontalDivider()
        }
    }
}
```

**New:** _(file deleted — `GoalsScreen` and `GoalCard` replace it; its only caller was the old host tab, removed in Step 10)_

#### `shared/ui/goal/src/commonMain/kotlin/org/orev/nahidka/ui/goal/mock/GoalsMockData.kt` — new

All demo records live in one file, as `TasksMockData` does: five goals covering emoji and letter pictures, deadlines, and a completed goal. The host seeds them once per session.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal.mock

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.service.GoalsManager

object GoalsMockData {

    suspend fun seed(goalsManager: GoalsManager) {
        listOf(
            GoalCreationRequest(
                identifier = "demo-half-marathon",
                title = "Run a half marathon",
                description = "Train three times a week and finish 21 km in under two hours.",
                picture = GoalPicture.Emoji("🏃"),
                progressPercentage = 40f,
                deadlineDate = LocalDate(2027, 4, 18),
            ),
            GoalCreationRequest(
                identifier = "demo-reading",
                title = "Read 24 books this year",
                description = "Two books a month, alternating fiction and non-fiction.",
                picture = GoalPicture.Emoji("📚"),
                progressPercentage = 75f,
                deadlineDate = LocalDate(2026, 12, 31),
            ),
            GoalCreationRequest(
                identifier = "demo-japan",
                title = "Save for a trip to Japan",
                description = "Put money aside every month for flights and two weeks of travel.",
                picture = GoalPicture.Emoji("🧳"),
                progressPercentage = 30f,
                deadlineDate = LocalDate(2027, 3, 1),
            ),
            GoalCreationRequest(
                identifier = "demo-guitar",
                title = "Learn to play the guitar",
                description = "Practice twenty minutes a day and learn five favorite songs.",
                picture = GoalPicture.Emoji("🎸"),
                progressPercentage = 100f,
            ),
            GoalCreationRequest(
                identifier = "demo-cooking",
                title = "Cook something new every week",
                description = "Try a recipe from a different cuisine each weekend.",
                progressPercentage = 10f,
            ),
        ).forEach { goalCreationRequest -> goalsManager.createGoal(goalCreationRequest) }
    }
}
```

#### `shared/ui/goal/README.md` — new

Module overview and verification commands, like `shared/ui/tasks/README.md`.

**Old:** _(file does not exist)_

**New:**

~~~~markdown
# Goals UI

`GoalsScreen` lists goal cards: a picture (an emoji, a photo from the device, or the title's first letter), the title, “Completed on N%”, the description (up to three lines), a progress bar and the optional deadline. Clicking a card opens the editor; the card's ⋮ menu has Edit and Delete (with confirmation). The editor holds the title, description, picture (12 emoji or **Choose photo**), a 0–100 % progress slider in 5 % steps and an optional deadline. Layouts use the screen width with the shared 600 dp breakpoint. Strings are available in English, Ukrainian, and Russian.

Photos are picked by `rememberPhotoPicker` from `shared/ui/common`: the Android Photo Picker, a desktop file dialog, a browser file input, or the iOS `PHPickerViewController`. The core keeps the original bytes; cards decode them off the main thread and scale them to at most 512 px.

`GoalsScreenGraph` uses the same `GoalsBindings` as `GoalsSessionGraph`. The app retains the ViewModel in its session and exposes a Goals entry.

## Temporary data

All production demo records live in [GoalsMockData.kt](src/commonMain/kotlin/org/orev/nahidka/ui/goal/mock/GoalsMockData.kt). `App.kt` invokes its seed function once for each remembered goals graph: five goals covering emoji and letter pictures, descriptions, optional deadlines, and a completed goal. Changes last for the app session; restarting creates fresh demo data.

To remove the demo seed, delete `GoalsMockData.kt` and remove its import and `LaunchedEffect` call from `App.kt`. Persistence and the backend remain outside this implementation.

## Verification

```sh
./gradlew :shared:core:feature:goals:jvmTest --rerun :shared:ui:common:jvmTest --rerun :shared:ui:goal:jvmTest --rerun
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
./gradlew -Pkotlin.native.enableKlibsCrossCompilation=true :shared:ui:common:compileKotlinIosArm64 :shared:ui:goal:compileKotlinIosArm64
```

`GoalsLayoutTest` renders desktop light and dark lists, the editor, and Ukrainian phone layouts at a true 360 dp viewport, including a photo goal. PNG previews are written to `/tmp/nahidka-goals-previews`.

The photo pickers open native system UI, so tests cover photos at the ViewModel and rendering level, not the picker dialogs themselves. iOS compiles on Linux through klib cross-compilation but was not run.
~~~~

### Step 9 — Tests (`src/jvmTest`)

They use the production `GoalsScreenGraph`, real clicks, text input and the slider's accessibility action. `GoalsLayoutTest` produced the previews in §1.

#### `shared/ui/goal/src/jvmTest/kotlin/org/orev/nahidka/ui/goal/GoalsTest.kt` — new

Shared setup: the graph, the main dispatcher, and helpers.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.dto.GoalRecord
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class GoalsTest {

    protected val goalsScreenGraph by lazy { createGraph<GoalsScreenGraph>() }
    protected val goalsViewModel by lazy { goalsScreenGraph.goalsViewModel }

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun createGoal(title: String, picture: GoalPicture? = null) {
        goalsViewModel.openGoalCreation()
        goalsViewModel.goalEditor.edit { goalDraft -> goalDraft.copy(title = title, picture = picture) }
        goalsViewModel.goalEditor.submit()
    }

    protected fun currentGoal(): GoalRecord? =
        goalsViewModel.goalsState.value.goals
            .singleOrNull()

    protected suspend fun awaitSingleGoal(goalExpectation: (GoalRecord) -> Boolean): GoalRecord =
        goalsViewModel.goalsState
            .first { goalsSnapshot ->
                goalsSnapshot.goals
                    .singleOrNull()
                    ?.let(goalExpectation) == true
            }
            .goals
            .single()
}
```

#### `shared/ui/goal/src/jvmTest/kotlin/org/orev/nahidka/ui/goal/GoalsViewModelTest.kt` — new

Creation with every field, blank-title validation, an emoji replaced by a photo, and deletion.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.goals.dto.GoalPicture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GoalsViewModelTest : GoalsTest() {

    @Test
    fun goalCreationSavesEveryField() = runTest {
        val deadlineDate = LocalDate(2027, 4, 18)

        goalsViewModel.openGoalCreation()
        goalsViewModel.goalEditor.edit { goalDraft ->
            goalDraft.copy(
                title = "  Run a half marathon  ",
                description = "Finish 21 km",
                picture = GoalPicture.Emoji("🏃"),
                progressPercentage = 40f,
                deadlineDate = deadlineDate,
            )
        }
        goalsViewModel.goalEditor.submit()

        val createdGoal = awaitSingleGoal { true }
        assertEquals("Run a half marathon", createdGoal.title)
        assertEquals("Finish 21 km", createdGoal.description)
        assertEquals(GoalPicture.Emoji("🏃"), createdGoal.picture)
        assertEquals(40f, createdGoal.progressPercentage)
        assertEquals(deadlineDate, createdGoal.deadlineDate)
        assertNull(goalsViewModel.goalEditor.dialogState.value)
    }

    @Test
    fun goalDraftWithBlankTitleIsNotSubmittable() {
        goalsViewModel.openGoalCreation()
        goalsViewModel.goalEditor.edit { goalDraft -> goalDraft.copy(title = "  ") }

        assertFalse(checkNotNull(goalsViewModel.goalEditor.dialogState.value).submittable)
    }

    @Test
    fun goalEditingReplacesPictureWithPhotoAndUpdatesProgress() = runTest {
        createGoal("Run a half marathon", GoalPicture.Emoji("🏃"))
        val createdGoal = awaitSingleGoal { true }

        goalsViewModel.openGoalEditing(createdGoal)
        goalsViewModel.goalEditor.edit { goalDraft ->
            goalDraft.copy(picture = GoalPicture.Photo(byteArrayOf(1, 2, 3)), progressPercentage = 100f)
        }
        goalsViewModel.goalEditor.submit()

        val editedGoal = awaitSingleGoal { goal -> goal.progressPercentage == 100f }
        assertEquals(GoalPicture.Photo(byteArrayOf(1, 2, 3)), editedGoal.picture)
        assertEquals(createdGoal.identifier, editedGoal.identifier)
    }

    @Test
    fun goalDeletionRemovesGoal() = runTest {
        createGoal("Run a half marathon")
        val createdGoal = awaitSingleGoal { true }

        goalsViewModel.goalDeletion.open(createdGoal)
        goalsViewModel.goalDeletion.submit()

        goalsViewModel.goalsState.first { goalsSnapshot -> goalsSnapshot.goals.isEmpty() }
    }
}
```

#### `shared/ui/goal/src/jvmTest/kotlin/org/orev/nahidka/ui/goal/GoalsScreenTest.kt` — new

Creation through the dialog (title, description, emoji, slider), card click, clearing an emoji, cancelled and confirmed deletion, and the compact ＋ button.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.GoalPicture
import kotlin.test.Test
import kotlin.test.assertEquals

private const val GOAL_TITLE = "Run a half marathon"

@OptIn(ExperimentalTestApi::class)
class GoalsScreenTest : GoalsTest() {

    @Test
    fun creationDialogSavesTitleDescriptionEmojiAndProgress() = runComposeUiTest {
        showGoalsScreen(width = 1200.dp)
        onNodeWithText("Create goal")
            .performClick()
        onNodeWithText("Save")
            .assertIsNotEnabled()
        onNodeWithText("Title")
            .performTextInput(GOAL_TITLE)
        onNodeWithText("Description")
            .performTextInput("Finish 21 km")
        onNodeWithText("🏃")
            .performClick()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(50f) }
        onNodeWithText("Progress: 50%")
            .assertIsDisplayed()
        onNodeWithText("Save")
            .performClick()

        onNodeWithText("Completed on 50%")
            .assertIsDisplayed()
        waitUntil { currentGoal()?.picture == GoalPicture.Emoji("🏃") }
        assertEquals("Finish 21 km", currentGoal()?.description)
    }

    @Test
    fun clickingCardOpensGoalEditor() = runComposeUiTest {
        createGoal(GOAL_TITLE)
        showGoalsScreen(width = 1200.dp)

        onNodeWithText(GOAL_TITLE)
            .performClick()

        onNodeWithText("Edit goal")
            .assertIsDisplayed()
    }

    @Test
    fun choosingSelectedEmojiAgainRemovesPicture() = runComposeUiTest {
        createGoal(GOAL_TITLE, GoalPicture.Emoji("🎯"))
        showGoalsScreen(width = 1200.dp)
        onNodeWithText(GOAL_TITLE)
            .performClick()

        onAllNodesWithText("🎯")
            .onLast()
            .performClick()
        onNodeWithText("Save")
            .performClick()

        waitUntil { currentGoal()?.picture == null }
    }

    @Test
    fun deletingGoalRequiresConfirmationAndCanBeCancelled() = runComposeUiTest {
        createGoal(GOAL_TITLE)
        showGoalsScreen(width = 400.dp)

        openGoalMenuAndChooseDelete()
        onNodeWithText("“$GOAL_TITLE” will be deleted.")
            .assertIsDisplayed()
        onNodeWithText("Cancel")
            .performClick()
        onNodeWithText(GOAL_TITLE)
            .assertIsDisplayed()

        openGoalMenuAndChooseDelete()
        onNodeWithText("Delete")
            .performClick()

        onNodeWithText("There are no goals yet")
            .assertIsDisplayed()
    }

    @Test
    fun compactCreateButtonShowsIconAndOpensCreationDialog() = runComposeUiTest {
        createGoal(GOAL_TITLE)
        showGoalsScreen(width = 360.dp)

        onNodeWithContentDescription("Create goal")
            .performClick()

        onNodeWithText("New goal")
            .assertIsDisplayed()
    }

    private fun ComposeUiTest.openGoalMenuAndChooseDelete() {
        onNodeWithContentDescription("More actions")
            .performClick()
        onNodeWithText("Delete")
            .performClick()
    }

    private fun ComposeUiTest.showGoalsScreen(width: Dp) {
        setContent {
            GoalsScreen(goalsViewModel, Modifier.size(width, 800.dp))
        }
    }
}
```

#### `shared/ui/goal/src/jvmTest/kotlin/org/orev/nahidka/ui/goal/GoalsLayoutTest.kt` — new

Renders desktop light and dark, the editor, and Ukrainian 360 dp layouts, with a generated photo, into `/tmp/nahidka-goals-previews`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.goal.mock.GoalsMockData
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

private const val PHOTO_GOAL_TITLE = "Climb Hoverla at sunrise"
private const val SAMPLE_PHOTO_SIZE = 1024

@OptIn(ExperimentalTestApi::class)
class GoalsLayoutTest : GoalsTest() {

    @Test
    fun expandedLayoutsAndEditorRenderWithDemoData() = runSkikoComposeUiTest(size = Size(1200f, 800f)) {
        seedGoalsWithPhoto()
        val darkTheme = mutableStateOf(false)
        setContent {
            NahidkaTheme(darkTheme.value) {
                Surface {
                    GoalsScreen(
                        goalsViewModel,
                        Modifier
                            .size(1200.dp, 800.dp)
                            .testTag("goals-preview"),
                    )
                }
            }
        }
        awaitPhoto()
        onNodeWithText("Completed on 100%")
            .assertIsDisplayed()
        saveScreen("desktop-goals-light")
        darkTheme.value = true
        saveScreen("desktop-goals-dark")
        darkTheme.value = false
        onNodeWithText("Run a half marathon")
            .performClick()
        onNodeWithText("Edit goal")
            .assertIsDisplayed()
        saveDialog("desktop-editor")
    }

    @Test
    fun ukrainianCompactLayoutsFitAt360Dp() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("uk"))
        try {
            runSkikoComposeUiTest(size = Size(360f, 800f)) {
                seedGoalsWithPhoto()
                setContent {
                    NahidkaTheme {
                        Surface {
                            GoalsScreen(
                                goalsViewModel,
                                Modifier
                                    .size(360.dp, 800.dp)
                                    .testTag("goals-preview"),
                            )
                        }
                    }
                }
                awaitPhoto()
                onNodeWithContentDescription("Створити ціль")
                    .assertIsDisplayed()
                onNodeWithText("Виконано на 40%")
                    .assertIsDisplayed()
                saveScreen("phone-goals-uk")
                onNodeWithText("Run a half marathon")
                    .performClick()
                onNodeWithText("Редагування цілі")
                    .assertIsDisplayed()
                onNodeWithText("Зберегти")
                    .assertIsDisplayed()
                saveDialog("phone-editor-uk")
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private suspend fun seedGoalsWithPhoto() {
        goalsScreenGraph.goalsManager.createGoal(
            GoalCreationRequest(
                identifier = "photo-goal",
                title = PHOTO_GOAL_TITLE,
                description = "Reach the highest peak of Ukraine before the first light.",
                picture = GoalPicture.Photo(samplePhotoContent()),
                progressPercentage = 60f,
            ),
        )
        GoalsMockData.seed(goalsScreenGraph.goalsManager)
    }

    private fun samplePhotoContent(): ByteArray {
        val photoSize = SAMPLE_PHOTO_SIZE.toFloat()
        val photoBitmap = ImageBitmap(SAMPLE_PHOTO_SIZE, SAMPLE_PHOTO_SIZE)

        Canvas(photoBitmap).drawRect(
            rect = Rect(0f, 0f, photoSize, photoSize),
            paint = Paint().apply {
                shader = LinearGradientShader(
                    from = Offset.Zero,
                    to = Offset(0f, photoSize),
                    colors = listOf(Color(0xFFF6B26B), Color(0xFF7B42D0), Color(0xFF151523)),
                )
            },
        )

        return photoBitmap.encodeToPng()
    }

    private fun ComposeUiTest.awaitPhoto() {
        waitUntil {
            onAllNodesWithContentDescription(PHOTO_GOAL_TITLE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun ComposeUiTest.saveScreen(name: String) {
        onNodeWithTag("goals-preview")
            .saveImage(name)
    }

    private fun ComposeUiTest.saveDialog(name: String) {
        onNode(isDialog())
            .saveImage(name)
    }

    private fun SemanticsNodeInteraction.saveImage(name: String) {
        val outputDirectory = File("/tmp/nahidka-goals-previews")
        assertTrue(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name.png").writeBytes(captureToImage().encodeToPng())
    }

    private fun ImageBitmap.encodeToPng(): ByteArray {
        val image = Image.makeFromBitmap(asSkiaBitmap())
        val encodedImage = checkNotNull(image.encodeToData())
        val pngContent = encodedImage.bytes
        encodedImage.close()
        image.close()
        return pngContent
    }
}
```

### Step 10 — Host integration (`shared/`)

Replaces the hand-built `GoalsContext` / `GoalsManager` and the old goals tab with the graph, a session-retained ViewModel and `GoalsScreen`. This is the same wiring as Tasks. The side menu itself stays out of scope.

#### `shared/src/commonMain/kotlin/org/orev/nahidka/App.kt` — modified

Adds a **Goals** top-bar entry. Overview and the personal-feature entries reset `showingGoals`. Settings and Tasks come earlier in the `if` chain, the precedence the existing flags already rely on. `identifierGenerator` is removed because it only served the old goals tab.

_Change 1 of 7 (old line 21 / new line 21)_

**Old:**

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.core.common.SystemApplicationClock
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.di.rememberFinancialSession
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.service.SettingsContext
```

**New:**

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.orev.nahidka.core.common.SystemApplicationClock
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.di.rememberFinancialSession
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.service.SettingsContext
```

_Change 2 of 7 (old line 43 / new line 38)_

**Old:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.tasks.TasksScreen
```

**New:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.goal.GoalsScreen
import org.orev.nahidka.ui.goal.GoalsScreenGraph
import org.orev.nahidka.ui.goal.GoalsViewModel
import org.orev.nahidka.ui.goal.mock.GoalsMockData
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.tasks.TasksScreen
```

_Change 3 of 7 (old line 78 / new line 77)_

**Old:**

```kotlin
        TasksMockData.seed(tasksScreenGraph.tasksManager)
    }
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
    val batteryContext = remember(session) { SocialBatteryContext() }
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val goalsSnapshot by goalsContext.goalsState.collectAsState()
    val batterySnapshot by batteryContext.batteryState.collectAsState()
    var selectedPersonalFeature by remember { mutableStateOf<PersonalFeature?>(null) }
```

**New:**

```kotlin
        TasksMockData.seed(tasksScreenGraph.tasksManager)
    }
    val goalsScreenGraph = remember(session) { createGraph<GoalsScreenGraph>() }
    LaunchedEffect(goalsScreenGraph) {
        GoalsMockData.seed(goalsScreenGraph.goalsManager)
    }
    val batteryContext = remember(session) { SocialBatteryContext() }
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val batterySnapshot by batteryContext.batteryState.collectAsState()
    var selectedPersonalFeature by remember { mutableStateOf<PersonalFeature?>(null) }
```

_Change 4 of 7 (old line 103 / new line 102)_

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
    var showingGoals by remember { mutableStateOf(false) }

    val dashboardViewModel = viewModel<DashboardViewModel>(
```

_Change 5 of 7 (old line 118 / new line 118)_

**Old:**

```kotlin
    }

    val financialHistoryViewModel = viewModel<FinancialHistoryViewModel>(
        viewModelStoreOwner = session,
```

**New:**

```kotlin
    }

    val goalsViewModel = viewModel<GoalsViewModel>(
        viewModelStoreOwner = session,
        key = "goals",
    ) {
        goalsScreenGraph.goalsViewModel
    }

    val financialHistoryViewModel = viewModel<FinancialHistoryViewModel>(
        viewModelStoreOwner = session,
```

_Change 6 of 7 (old line 137 / new line 144)_

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
                TextButton(onClick = { showingSettings = false; showingFinance = false; showingTasks = false; showingGoals = false; selectedPersonalFeature = null }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true; selectedPersonalFeature = null }) { Text("Settings") }
                TextButton(onClick = { showingSettings = false; showingTasks = true; selectedPersonalFeature = null }) { Text("Tasks") }
                TextButton(onClick = { showingSettings = false; showingTasks = false; showingGoals = true; selectedPersonalFeature = null }) { Text("Goals") }
                PersonalFeature.entries.forEach { feature ->
                    TextButton(onClick = { showingSettings = false; showingTasks = false; showingGoals = false; selectedPersonalFeature = feature }) {
                        Text(feature.name.lowercase().replace('_', ' '))
                    }
```

_Change 7 of 7 (old line 162 / new line 170)_

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
```

**New:**

```kotlin
                } else if (showingTasks) {
                    TasksScreen(tasksViewModel)
                } else if (showingGoals) {
                    GoalsScreen(goalsViewModel)
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        batteryPercentage = batterySnapshot.socialBattery?.percentage,
                        onUpdateBattery = { percentage -> mutatePersonalFeature { batteryManager.updateBattery(SocialBattery(percentage)) } },
                        errorMessage = personalFeatureError,
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/PersonalFeature.kt` — modified

`GOALS` leaves, as `TASKS` did.

**Old:**

```kotlin
package org.orev.nahidka

internal enum class PersonalFeature { GOALS, SOCIAL_BATTERY }
```

**New:**

```kotlin
package org.orev.nahidka

internal enum class PersonalFeature { SOCIAL_BATTERY }
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/PersonalFeaturesScreen.kt` — modified

Only the goals parameters and branch are removed. The social-battery code is unchanged.

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
import org.orev.nahidka.ui.socialbattery.SocialBatteryWidget

@Composable
internal fun PersonalFeaturesScreen(
    feature: PersonalFeature,
    batteryPercentage: Int?,
    onUpdateBattery: (Int) -> Unit,
    errorMessage: String?,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
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

#### `shared/src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionGraph.kt` — modified

`IdentifierGeneratorBindings` instead of its own copy of the provider.

_Change 1 of 2 (old line 15 / new line 15)_

**Old:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel

@DependencyGraph(FinancialSessionScope::class)
interface FinancialSessionGraph {
    val finance: FinancialModule
```

**New:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel

@DependencyGraph(FinancialSessionScope::class, bindingContainers = [IdentifierGeneratorBindings::class])
interface FinancialSessionGraph {
    val finance: FinancialModule
```

_Change 2 of 2 (old line 37 / new line 37)_

**Old:**

```kotlin
    fun provideApplicationClock(): ApplicationClock = SystemApplicationClock()

    @Provides
    fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()

    @DependencyGraph.Factory
    interface Factory {
```

**New:**

```kotlin
    fun provideApplicationClock(): ApplicationClock = SystemApplicationClock()

    @DependencyGraph.Factory
    interface Factory {
```

#### `shared/src/jvmTest/kotlin/org/orev/nahidka/GoalsHostTest.kt` — new

Opens the real app, edits a demo goal, leaves, and comes back: the edit is preserved.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka

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
class GoalsHostTest {

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun goalsEntryUsesDemoDataAndPreservesEditsAcrossNavigation() = runComposeUiTest {
        setContent { App() }
        onNodeWithText("Goals")
            .performClick()
        onNodeWithText("Run a half marathon")
            .assertIsDisplayed()
        onNodeWithText("Run a half marathon")
            .performClick()
        onNodeWithText("Title")
            .performTextReplacement("Run a full marathon")
        onNodeWithText("Save")
            .performClick()
        onNodeWithText("Run a full marathon")
            .assertIsDisplayed()
        onNodeWithText("Overview")
            .performClick()
        onNodeWithText("Goals")
            .performClick()
        onNodeWithText("Run a full marathon")
            .assertIsDisplayed()
    }
}
```

## 5. Verification, limitations, out of scope

**Verified (in an isolated copy of the repository at `6df8e9d`):**
* **52 / 52 tests pass, green on 3 consecutive forced re-runs** (`--rerun` on every test task). The 18 new ones are 4 `GoalsContextTest`, 2 `MutationDialogControllerTest`, 4 `GoalsViewModelTest`, 5 `GoalsScreenTest`, 2 `GoalsLayoutTest` and 1 `GoalsHostTest`. The 34 existing ones were re-run because Steps 2–3 touch shared code: 3 `DialogControllerTest`, 10 tasks core, 20 tasks UI (including real mouse and touch drags and the rendered layouts) and `TasksHostTest`.
* **Negative check:** with chip deselection disabled in `ChoiceChips` and the picture patch dropped from `GoalDraft`, the matching tests fail: `TasksScreenTest.choosingSelectedReactionAgainRemovesRating`, `GoalsScreenTest.choosingSelectedEmojiAgainRemovesPicture` and `GoalsViewModelTest.goalEditingReplacesPictureWithPhotoAndUpdatesProgress`. The source was restored before the final runs.
* `:shared:core:feature:common`, `:shared:core:feature:goals`, `:shared:ui:common`, `:shared:ui:tasks` and `:shared:ui:goal` compile for **JVM, JS, Wasm, Android, iosArm64 and iosSimulatorArm64**, with main and JVM test sources. iOS klibs were built on Linux with `-Pkotlin.native.enableKlibsCrossCompilation=true`, which caught and fixed one iOS interop mistake in the picker.
* `:androidApp:assembleDebug`, `:desktopApp:compileKotlin`, `:webApp:compileKotlinJs`, `:webApp:compileKotlinWasmJs` and `:shared:jvmTest` all succeed.
* No new compiler warnings, and the Metro warning on `GoalsManager` is gone. The warnings that remain are the existing `@Inject constructor` ones in untouched files (`core:common`, financial, settings, social battery). `git diff --check` is clean.
* The main README example of the goals core was executed as a temporary test and passed.
* The previews in §1 were rendered from this code and inspected: desktop light and dark, the editor, and Ukrainian phone layouts at a true 360 dp, including a photo goal.

**Not verified:**
* **The four photo pickers with real system UI.** The Android Photo Picker, desktop `FileDialog`, browser file input and iOS `PHPickerViewController` open native windows that UI tests can't drive. Tests cover photos from the draft onward: saving, content equality, decoding and rendering.
* **iOS at runtime.** The klibs compile, but linking and running need macOS.
* **A physical touchscreen.** Touch was tested with Compose's synthetic input.

**Known limitations:**
* Goals live in memory for the app session, as the core does today. Persistence and the backend are not part of this plan.
* Photos keep their original size in memory; only the displayed bitmap is scaled down.
* Desktop and web can't decode HEIC. Their pickers offer only PNG, JPEG, WebP, GIF and BMP. Android decodes HEIC natively, and iOS converts every photo to JPEG.
* The desktop file filter is ignored on Windows, where `FileDialog` lists every file. A file that isn't an image leaves the picture box empty instead of failing.
* The slider snaps to 5 % steps, so a value set elsewhere, such as 33 %, snaps once the slider is moved.

**Applying this together with the pending social-battery plan:**
* `LayoutWidth.kt` and the `TasksScreen.kt` padding change are **identical** in both plans. Apply them once.
* Both plans edit `App.kt`, `PersonalFeature.kt` and `PersonalFeaturesScreen.kt`. Each removes its own feature from `PersonalFeature`, so once **both** are applied the enum is empty. Then delete `PersonalFeature.kt` and `PersonalFeaturesScreen.kt`, and in `App.kt` remove `selectedPersonalFeature`, `personalFeatureError`, `mutatePersonalFeature`, the `PersonalFeature.entries` loop, the `selectedPersonalFeature != null` branch, and the `selectedPersonalFeature = …` assignments in the top-bar buttons. *I can write that merged host step out in full once you've decided on both plans.*

**Out of scope, possible follow-ups:**
* **The side menu** and the app shell.
* **The dashboard's “Shared Goals” / “Savings Goal” cards.** They show hard-coded sample data and aren't connected to this core.
* **Shrinking stored photos** (needs a per-platform encoder) and **persistence**.
* **Sorting, filtering and an overdue highlight.**
* **A test-support module.** `TasksLayoutTest`, `GoalsLayoutTest` and the social-battery layout test each carry the same ~15-line preview-capture helpers. Kotlin Multiplatform has no test fixtures, so sharing them needs a small module.
* **The financial UI migration to `ui:common`** (pending revision of its plan). Its `Financial*` copies of these components remain.

**Suggested implementation order:** Steps 1 → 10. The core compiles and its tests pass after Step 1. `ui/common` compiles and its tests pass after Step 2. Tasks still passes after Step 3. The goal module compiles after Steps 4–8, its tests run after Step 9, and the app runs after Step 10. You can delete `plan-previews/` once you've reviewed the plan; nothing references it except this file.
