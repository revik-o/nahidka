# Implementation plan — `shared/ui/tasks`

**Implementation status (2026-10-05):** all eleven steps are implemented in the current worktree, with centralized demo data. **33 module tests passed on three consecutive forced runs**, and the actual app navigation test passed. Desktop, Android, JS, and Wasm app build checks passed. The drag and reaction-removal negative checks failed as expected, and production behavior was restored. Desktop and Ukrainian phone layouts were rendered and inspected. See [the completion audit](IMPLEMENTATION_STATUS.md) for current evidence; the original planning notes below describe the repository before implementation.

**Revision 2:** ratings are shown as **reactions** (😞 😐 🙂 🤩) instead of stars, and the reactions are customizable (§2.1).

---

## 1. What gets built (mockup → implementation)

| Mockup element | Implementation |
|---|---|
| `board (Selected)` / `list view` | Segmented selector **Board / List**, right-aligned as in the mockup; the choice survives configuration changes (`rememberSaveable`) |
| `Create new task` | Opens the task editor: title, description, status, due date (optional), and a reaction rating (Done only) |
| Columns `To Do` / `In Progress` / `Done` | Three equal columns with counts (`To Do (3)`); an empty column shows "No tasks" |
| Card: `issue title`, `Task description…`, `Due date` | Title (up to 2 lines), description cut with `…` after 3 lines, `📅 05.10.2026`. Done cards also show their reaction (🤩), or a "Rate" button |
| Moving cards *(not drawn)* | **Drag & drop** between columns: a mouse drags immediately, touch drags after a long press. The card follows the pointer, the source card fades, and the target column is outlined. Every card's ⋮ menu also has "Move to …" |
| Card actions *(not drawn)* | Clicking a card opens the editor; ⋮ → Move to … / Edit / Delete (with confirmation) |
| List view (Jira-like) | **Title · Description** (one line, ends with `…`) **· Status** badge (click → "Move to …") **· Due date · Rating** reaction (Done tasks only; click → reaction bar `😞 😐 🙂 🤩`; tap the chosen reaction again to remove it) **· ⋮** Edit / Delete; the header row stays visible while scrolling |
| Customizable ratings | Header ⋮ → **Rating reactions**: change, reorder, add, and delete reactions. Defaults 😞 bad · 😐 okay · 🙂 good · 🤩 excellent |
| SIDE MENU | App shell, outside this module (unchanged). The temporary host top bar gets a "Tasks" entry (Step 11) |

### Mobile adaptivity

The screen measures **its own width** (`BoxWithConstraints`), so a narrow desktop window behaves like a phone. The breakpoint is 600 dp (Material "compact"), shared through `shared/ui/common`'s `LayoutWidth`.

| | Expanded (≥ 600 dp: desktop, web, tablets) | Compact (< 600 dp: phones) |
|---|---|---|
| Header | One right-aligned row: `[Board │ List]` `[Create new task]` `[⋮]` | One row: `[Board │ List]` fills the width, then `[＋]` `[⋮]` |
| Board | Three columns (1200 dp max), drag & drop (long press on tablets) | **Scrollable status tabs** `To Do (3) │ In Progress (1) │ Done (2)` + one column at full width. Moves use ⋮ → "Move to …": drag & drop needs at least two visible columns |
| List | Table with a sticky header and weighted columns | Rows: title / description `…` / `[STATUS] 📅 date rating` (the last line wraps when space runs out) + ⋮ |
| Dialogs | Material `AlertDialog` | Same; the content scrolls |
| Padding | 24 dp | 16 dp |

Checked on 400 dp and on 360 dp in Ukrainian (the longest labels): no clipped controls; the tabs scroll instead of truncating "До виконання (3)".

---

## 2. Decisions

**Confirmed in chat**

1. **Ratings are reactions, not stars (revision 2).** A rating level is one reaction, worst first: an emoji, or any short text such as `👍 Good`. The defaults are `😞` bad · `😐` okay · `🙂` good · `🤩` excellent. Emoji are language-neutral, so the core holds no localized text, and users can add words in their own language. Tasks store the reaction's identifier, so changing a reaction updates every rated task. To rate, tap "Rate" to open the reaction bar; tapping the chosen reaction again removes the rating. The task editor shows the same reaction bar.
2. **Customization happens in a dialog on the tasks screen** (header ⋮ → Rating reactions).
3. **Drag & drop plus the ⋮ "Move to …" menu.**
4. **A new `shared/ui/common` module** holds the generic building blocks (Step 2).

**Decisions I made — please confirm or redirect**

1. **Rating rules live in the core, once.** `TaskStatus.acceptsRating` is `true` only for `DONE`. Moving a task out of Done clears its rating, whether by drag & drop, menu, or editor. Rating a task that isn't Done is rejected. Deleting a reaction clears it from the tasks rated with it in the same revision. The UI only reads `acceptsRating` to decide when to show rating controls.
2. **The due date is optional and date-only** (`kotlinx.datetime.LocalDate`), with no time and no "overdue" highlight. Highlighting needs a clock and a time zone in the tasks core. *If you want overdue dates in red, it's a small addition.*
3. **Order inside a column is creation order** (the core snapshot order). Drag & drop changes the status, not the position: the core has no rank field. The UI doesn't expose `priority` because the mockup doesn't show it.
4. **List columns:** you asked for title, description, status, and rating. I added **Due date**, because Jira's list has it and the cards show it. There's no sorting or filtering, since neither is in the mockup.
5. **Errors:** the core rejects invalid input with `IllegalArgumentException`. Dialogs show "This change could not be saved" inside the dialog; quick actions (drag & drop, menus) show a snackbar, so a failure never crashes the app. The UI validates only to enable Save: the title can't be blank, and level titles can't be blank.
6. **DI:** Metro rejects one graph extending another, so the core's providers move into a `@BindingContainer` (`TasksBindings`). Both `TasksSessionGraph` and the new `TasksScreenGraph` include it. The host and the tests use the same `TasksScreenGraph`, so the tests check the production wiring.
7. **Host:** tasks get their own entry in the temporary top bar, and `PersonalFeature` loses `TASKS` (its old `TasksTable` was deleted in `409e1fe`).
8. **Strings:** en / uk / ru, all translated.

**How your style rules are applied:** there are no comments in any source file, tests included. Names are full words. New code has no implicit `it`: every lambda parameter is named. Builder, flow, `Modifier`, and test-finder chains put one call per line. `@Inject` is class-level, which also fixes the existing Metro warning on `TasksManager`. Imports follow the IntelliJ layout (wildcards from 5 names, `kotlin.*` last). The new code adds no compiler warnings on any target.

---

## 3. Architecture

```
shared/core/feature/tasks            (Step 1)
├── dto/      TaskStatus(acceptsRating) · TaskRecord(+dueDate, +ratingIdentifier) · TaskRatingLevel(reaction)
│             DEFAULT_TASK_RATING_LEVELS · Task*Request(+due date, +rating) · TasksSnapshot(+ratingLevels)
├── service/  TasksContext — tasks + rating levels in one atomic state, all rating rules
│             TasksRepository / TasksManager — + replaceRatingLevels
└── di/       TasksBindings (shared providers) · TasksSessionGraph

shared/ui/common                     (Step 2, feature-agnostic)
├── layout/   LayoutWidth                       COMPACT | EXPANDED (600 dp)
├── component/ChoiceSelector · ChoiceField · AddButton · DropdownPopup · ActionsMenu · MoreActionsMenu · MenuAction
│             DateField · DataTable
├── format/   DAY_FORMAT
└── dialog/   DialogController<Draft, Rejection> · DialogState · ControlledDialog

shared/ui/tasks
├── TasksScreen · TasksHeader · TasksView         public entry point, header, Board | List
├── TasksViewModel · TasksScreenGraph             state + intents; DI composition root
├── model/     TaskItem · TasksContent · TaskDraft · TaskRatingLevelsDraft · TaskInteractions
├── component/ TaskStatusTitle · TaskStatusBadge · TaskStatusMenu · TaskRatingMenu · TaskRatingReactions
│              TaskMenuActions · TaskDueDateText
├── board/     TasksBoard · TaskBoardColumn · TaskCardList · TaskCard · TaskBoardDragState · TaskCardDragGesture
├── list/      TasksList · TaskListRow · TaskListColumn
└── dialog/    TaskDialogController · TaskDialog · TaskEditorDialog · TaskDeletionDialog · TaskRatingLevelsDialog
```

**Data flow**

* **Read:** `TasksRepository.tasksState` → `map(TasksContent::of)` → `StateFlow<TasksContent>` → board / list. There is no loading state, because the core `StateFlow` always has a value.
* **Dialogs:** Save → `DialogController.submit()` → `TasksManager` → success closes the dialog; a rejection shows inside it.
* **Quick changes** (drag & drop, "Move to …", reaction rating): `TaskInteractions` → `TasksViewModel.moveTask` / `rateTask` → `TasksManager.updateTasks`. On failure, `changeRejections` shows a snackbar. Every screen updates from the core state; nothing refreshes by hand.

**DRY map**

| Shared piece | Used by |
|---|---|
| `TaskStatus.acceptsRating` (core) | core validation, auto-clear on leaving Done, editor reaction bar, card and list rating buttons, editor draft |
| `NullablePatch.applyTo` / `nullablePatch` (`core:common`) | core description, due date and rating patches; UI update requests built from the draft |
| `TasksBindings` (core) | `TasksSessionGraph`, `TasksScreenGraph` (production and tests) |
| `DialogController` + `ControlledDialog` (`ui:common`) via `TaskDialogController` / `TaskDialog` | task editor, task deletion, rating reactions editor |
| `DropdownPopup` (`ui:common`) | `ActionsMenu` / `MoreActionsMenu` (card ⋮, list row ⋮, header ⋮, status badge, reaction ⋮), `TaskRatingMenu` (reaction bar) |
| `TaskMenuActions` | card ⋮, list row ⋮, status badge menu |
| `TaskRatingReactions` | rating popup on cards and list rows, task editor |
| `TaskStatusMenu`, `TaskRatingMenu`, `TaskDueDateText` | board cards, expanded list rows, compact list rows |
| `taskStatusTitleWithCount` | board column headers, compact status tabs |
| `TaskListColumn` | list header and list rows (titles and weights in one place) |
| `TaskCardList` + `TaskCard` | expanded columns, compact board, dragged card |
| `ChoiceSelector`, `ChoiceField`, `DateField`, `AddButton`, `DataTable`, `LayoutWidth`, `DAY_FORMAT` (`ui:common`) | tasks now; the financial plan later (§5) |

---

## 4. Implementation steps

Each file shows **Old** (empty for a new file) and **New**. Small or heavily rewritten files show the whole file; larger files show only the changed regions, with two lines of context.

### Step 1 — Core: repair the module, add due dates and customizable ratings (`shared/core/feature/tasks`)

Today this module **does not compile**: `TaskRecord` is a typealias to `org.orev.nahidka.api.Task` and `TaskStatus` lives in `org.orev.nahidka.api`, and both were deleted with `shared/core/lib/api` in `409e1fe`. Both now live in the tasks core. All rating rules are in the core, so the UI never duplicates them.

#### `shared/core/feature/tasks/build.gradle.kts` — modified

`LocalDate` for due dates.

_Change 1 of 1 (old line 31 / new line 31)_

**Old:**

```kotlin
            api(project(":shared:core:feature:common"))
            api(libs.kotlinx.coroutines.core)
            implementation(libs.metro.runtime)
        }
```

**New:**

```kotlin
            api(project(":shared:core:feature:common"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.metro.runtime)
        }
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TaskStatus.kt` — new

Moved from the deleted `api` module. `acceptsRating` is the single source of the “only Done can be rated” rule (core validation and UI both read it).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

enum class TaskStatus(val acceptsRating: Boolean) {
    TO_DO(acceptsRating = false),
    IN_PROGRESS(acceptsRating = false),
    DONE(acceptsRating = true)
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TaskRecord.kt` — modified

Typealias to the deleted `api.Task` → real data class with `dueDate` and `ratingIdentifier`.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

typealias TaskRecord = org.orev.nahidka.api.Task
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.datetime.LocalDate

data class TaskRecord(
    val identifier: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val priority: Int = 0,
    val dueDate: LocalDate? = null,
    val ratingIdentifier: String? = null
)
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TaskRatingLevel.kt` — new

One customizable rating reaction (`"🤩"`, or any short text such as `"👍 Good"`); list order = scale order, worst first.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

data class TaskRatingLevel(
    val identifier: String,
    val reaction: String
)
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/DefaultTaskRatingLevels.kt` — new

Default reactions 😞 bad · 😐 okay · 🙂 good · 🤩 excellent, so rating works before the user customizes anything. Emoji are language-neutral, so no localized text lives in the core.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

val DEFAULT_TASK_RATING_LEVELS: ImmutableList<TaskRatingLevel> = persistentListOf(
    TaskRatingLevel("bad", "😞"),
    TaskRatingLevel("okay", "😐"),
    TaskRatingLevel("good", "🙂"),
    TaskRatingLevel("excellent", "🤩")
)
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TaskCreationRequest.kt` — modified

Due date and rating on creation.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import org.orev.nahidka.api.TaskStatus

data class TaskCreationRequest(
    val identifier: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val priority: Int = 0
)
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.datetime.LocalDate

data class TaskCreationRequest(
    val identifier: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val priority: Int = 0,
    val dueDate: LocalDate? = null,
    val ratingIdentifier: String? = null
)
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TaskUpdateRequest.kt` — modified

Due date and rating patches reuse the existing `NullablePatch`.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import org.orev.nahidka.api.TaskStatus
import org.orev.nahidka.core.common.NullablePatch

data class TaskUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val descriptionPatch: NullablePatch<String> = NullablePatch.Keep,
    val status: TaskStatus? = null,
    val priority: Int? = null
)
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch

data class TaskUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val descriptionPatch: NullablePatch<String> = NullablePatch.Keep,
    val status: TaskStatus? = null,
    val priority: Int? = null,
    val dueDatePatch: NullablePatch<LocalDate> = NullablePatch.Keep,
    val ratingIdentifierPatch: NullablePatch<String> = NullablePatch.Keep
)
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TasksMutationResult.kt` — modified

`changed` becomes a property with the old default, so a level-only change (no task affected) still reports `changed = true`.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

data class TasksMutationResult(
    val revision: Long,
    val affectedTasks: ImmutableList<TaskRecord>
) {

    constructor(revision: Long, affectedTasks: List<TaskRecord>) : this(revision, affectedTasks.toPersistentList())

    val changed: Boolean get() = affectedTasks.isNotEmpty()
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

data class TasksMutationResult(
    val revision: Long,
    val affectedTasks: ImmutableList<TaskRecord>,
    val changed: Boolean = affectedTasks.isNotEmpty()
) {

    constructor(
        revision: Long,
        affectedTasks: List<TaskRecord>,
        changed: Boolean = affectedTasks.isNotEmpty()
    ) : this(revision, affectedTasks.toPersistentList(), changed)
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/dto/TasksSnapshot.kt` — modified

Snapshots carry the rating levels.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

data class TasksSnapshot(
    val revision: Long,
    private val recordsByIdentifier: PersistentMap<String, TaskRecord>
) {

    val tasks: ImmutableList<TaskRecord> by lazy {
        recordsByIdentifier.values.toPersistentList()
    }

    constructor(revision: Long, tasks: List<TaskRecord>) : this(
        revision, tasks.associateBy { it.identifier }.toPersistentMap()
    ) {
        require(tasks.size == recordsByIdentifier.size) { "Snapshot identifiers must be unique" }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

data class TasksSnapshot(
    val revision: Long,
    private val recordsByIdentifier: PersistentMap<String, TaskRecord>,
    val ratingLevels: ImmutableList<TaskRatingLevel>
) {

    val tasks: ImmutableList<TaskRecord> by lazy {
        recordsByIdentifier.values.toPersistentList()
    }

    constructor(
        revision: Long,
        tasks: List<TaskRecord>,
        ratingLevels: List<TaskRatingLevel> = DEFAULT_TASK_RATING_LEVELS
    ) : this(
        revision,
        tasks
            .associateBy { task -> task.identifier }
            .toPersistentMap(),
        ratingLevels.toPersistentList()
    ) {
        require(tasks.size == recordsByIdentifier.size) { "Snapshot identifiers must be unique" }
    }
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/service/TasksRepository.kt` — modified

`replaceRatingLevels` mutation.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot

interface TasksRepository {

    val tasksState: StateFlow<TasksSnapshot>

    suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult
    suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult
    suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.tasks.dto.*

interface TasksRepository {

    val tasksState: StateFlow<TasksSnapshot>

    suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult
    suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult
    suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult
    suspend fun replaceRatingLevels(ratingLevels: List<TaskRatingLevel>): TasksMutationResult
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/service/TasksContext.kt` — modified

Rating reactions (levels) in the same atomic state as tasks: validation, leaving Done clears the rating, removing a reaction clears it from rated tasks in the same revision. The description patch now reuses `applyTo` from `core:common` (DRY with the new due-date and rating patches).

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.tasks.dto.*

class TasksContext : TasksRepository {

    private val stateMutex = Mutex()
    private var tasksByIdentifier = persistentMapOf<String, TaskRecord>()
    private val mutableTasksState: MutableStateFlow<TasksSnapshot>

    override val tasksState: StateFlow<TasksSnapshot>

    constructor(initialTasks: List<TaskRecord> = emptyList()) {
        for (task in initialTasks) {
            validateTask(task)
            require(task.identifier !in tasksByIdentifier) { "Duplicate initial task identifier: ${task.identifier}" }
            tasksByIdentifier = tasksByIdentifier.putting(task.identifier, task)
        }

        mutableTasksState = MutableStateFlow(TasksSnapshot(0, tasksByIdentifier))
        tasksState = mutableTasksState.asStateFlow()
    }

    fun currentSnapshot(): TasksSnapshot = tasksState.value

    override suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val insertedTasks = ArrayList<TaskRecord>(requests.size)

        for (request in requests) {
            val task =
                TaskRecord(request.identifier, request.title, request.description, request.status, request.priority)

            validateTask(task)
            require(task.identifier !in nextTasks) {
                "Task already exists: ${task.identifier}"
            }

            nextTasks = nextTasks.putting(task.identifier, task)
            insertedTasks.add(task)
        }

        commit(nextTasks, insertedTasks)
    }

    override suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val deletedTasks = ArrayList<TaskRecord>(taskIdentifiers.size)

        for (identifier in taskIdentifiers) {
            require(identifier.isNotBlank()) { "Task identifier must not be blank" }
            val task = requireNotNull(nextTasks[identifier]) { "Unknown or duplicate task identifier: $identifier" }
            nextTasks = nextTasks.removing(identifier)
            deletedTasks.add(task)
        }

        commit(nextTasks, deletedTasks)
    }

    override suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val updatedTasks = ArrayList<TaskRecord>(requests.size)
        val identifiers = HashSet<String>(requests.size)

        for (request in requests) {
            require(identifiers.add(request.identifier)) {
                "Duplicate task identifier: ${request.identifier}"
            }

            val previousTask = requireNotNull(nextTasks[request.identifier]) {
                "Unknown task identifier: ${request.identifier}"
            }

            val currentTask = previousTask.copy(
                title = request.title ?: previousTask.title,
                description = when (val patch = request.descriptionPatch) {
                    NullablePatch.Keep -> previousTask.description
                    NullablePatch.Clear -> ""
                    is NullablePatch.Set -> patch.value
                },
                status = request.status ?: previousTask.status,
                priority = request.priority ?: previousTask.priority
            )

            validateTask(currentTask)

            if (previousTask != currentTask) {
                nextTasks = nextTasks.putting(currentTask.identifier, currentTask)
                updatedTasks.add(currentTask)
            }
        }

        commit(nextTasks, updatedTasks)
    }

    private suspend fun commit(
        nextTasks: kotlinx.collections.immutable.PersistentMap<String, TaskRecord>,
        affectedTasks: List<TaskRecord>
    ): TasksMutationResult {
        val previousSnapshot = mutableTasksState.value

        if (affectedTasks.isEmpty()) {
            return TasksMutationResult(previousSnapshot.revision, affectedTasks)
        }

        val nextRevision = incrementRevision(previousSnapshot.revision)
        val nextSnapshot = TasksSnapshot(nextRevision, nextTasks)
        val result = TasksMutationResult(nextRevision, affectedTasks)

        currentCoroutineContext().ensureActive()
        tasksByIdentifier = nextTasks
        mutableTasksState.value = nextSnapshot

        return result
    }

    private fun validateTask(task: TaskRecord) {
        require(task.identifier.isNotBlank()) { "Task identifier must not be blank" }
        require(task.title.isNotBlank()) { "Task title must not be blank" }
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.applyTo
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.tasks.dto.*

class TasksContext : TasksRepository {

    private val stateMutex = Mutex()
    private var tasksByIdentifier = persistentMapOf<String, TaskRecord>()
    private var ratingLevels: PersistentList<TaskRatingLevel>
    private val mutableTasksState: MutableStateFlow<TasksSnapshot>

    override val tasksState: StateFlow<TasksSnapshot>

    constructor(
        initialTasks: List<TaskRecord> = emptyList(),
        initialRatingLevels: List<TaskRatingLevel> = DEFAULT_TASK_RATING_LEVELS
    ) {
        validateRatingLevels(initialRatingLevels)
        ratingLevels = initialRatingLevels.toPersistentList()

        for (task in initialTasks) {
            validateTask(task)
            require(task.identifier !in tasksByIdentifier) { "Duplicate initial task identifier: ${task.identifier}" }
            tasksByIdentifier = tasksByIdentifier.putting(task.identifier, task)
        }

        mutableTasksState = MutableStateFlow(TasksSnapshot(0, tasksByIdentifier, ratingLevels))
        tasksState = mutableTasksState.asStateFlow()
    }

    fun currentSnapshot(): TasksSnapshot = tasksState.value

    override suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val insertedTasks = ArrayList<TaskRecord>(requests.size)

        for (request in requests) {
            val task = TaskRecord(
                request.identifier,
                request.title,
                request.description,
                request.status,
                request.priority,
                request.dueDate,
                request.ratingIdentifier
            )

            validateTask(task)
            require(task.identifier !in nextTasks) {
                "Task already exists: ${task.identifier}"
            }

            nextTasks = nextTasks.putting(task.identifier, task)
            insertedTasks.add(task)
        }

        commit(nextTasks, ratingLevels, insertedTasks)
    }

    override suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val deletedTasks = ArrayList<TaskRecord>(taskIdentifiers.size)

        for (identifier in taskIdentifiers) {
            require(identifier.isNotBlank()) { "Task identifier must not be blank" }
            val task = requireNotNull(nextTasks[identifier]) { "Unknown or duplicate task identifier: $identifier" }
            nextTasks = nextTasks.removing(identifier)
            deletedTasks.add(task)
        }

        commit(nextTasks, ratingLevels, deletedTasks)
    }

    override suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val updatedTasks = ArrayList<TaskRecord>(requests.size)
        val identifiers = HashSet<String>(requests.size)

        for (request in requests) {
            require(identifiers.add(request.identifier)) {
                "Duplicate task identifier: ${request.identifier}"
            }

            val previousTask = requireNotNull(nextTasks[request.identifier]) {
                "Unknown task identifier: ${request.identifier}"
            }

            val currentStatus = request.status ?: previousTask.status
            val currentTask = previousTask.copy(
                title = request.title ?: previousTask.title,
                description = request.descriptionPatch
                    .applyTo(previousTask.description)
                    .orEmpty(),
                status = currentStatus,
                priority = request.priority ?: previousTask.priority,
                dueDate = request.dueDatePatch.applyTo(previousTask.dueDate),
                ratingIdentifier = request.ratingIdentifierPatch.applyTo(
                    previousTask.ratingIdentifier.takeIf { currentStatus.acceptsRating }
                )
            )

            validateTask(currentTask)

            if (previousTask != currentTask) {
                nextTasks = nextTasks.putting(currentTask.identifier, currentTask)
                updatedTasks.add(currentTask)
            }
        }

        commit(nextTasks, ratingLevels, updatedTasks)
    }

    override suspend fun replaceRatingLevels(
        ratingLevels: List<TaskRatingLevel>
    ): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        validateRatingLevels(ratingLevels)
        val ratingLevelIdentifiers = ratingLevels.mapTo(HashSet(ratingLevels.size)) { ratingLevel ->
            ratingLevel.identifier
        }

        val unratedTasks = tasksByIdentifier.values
            .filter { task -> task.ratingIdentifier != null && task.ratingIdentifier !in ratingLevelIdentifiers }
            .map { task -> task.copy(ratingIdentifier = null) }

        val nextTasks = unratedTasks.fold(tasksByIdentifier) { nextTasks, task ->
            nextTasks.putting(task.identifier, task)
        }

        commit(nextTasks, ratingLevels.toPersistentList(), unratedTasks)
    }

    private suspend fun commit(
        nextTasks: PersistentMap<String, TaskRecord>,
        nextRatingLevels: PersistentList<TaskRatingLevel>,
        affectedTasks: List<TaskRecord>
    ): TasksMutationResult {
        val previousSnapshot = mutableTasksState.value

        if (affectedTasks.isEmpty() && nextRatingLevels == ratingLevels) {
            return TasksMutationResult(previousSnapshot.revision, affectedTasks)
        }

        val nextRevision = incrementRevision(previousSnapshot.revision)
        val nextSnapshot = TasksSnapshot(nextRevision, nextTasks, nextRatingLevels)
        val result = TasksMutationResult(nextRevision, affectedTasks, changed = true)

        currentCoroutineContext().ensureActive()
        tasksByIdentifier = nextTasks
        ratingLevels = nextRatingLevels
        mutableTasksState.value = nextSnapshot

        return result
    }

    private fun validateTask(task: TaskRecord) {
        require(task.identifier.isNotBlank()) { "Task identifier must not be blank" }
        require(task.title.isNotBlank()) { "Task title must not be blank" }

        task.ratingIdentifier?.let { ratingIdentifier ->
            require(task.status.acceptsRating) { "Task cannot be rated in status ${task.status}: ${task.identifier}" }
            require(ratingLevels.any { ratingLevel -> ratingLevel.identifier == ratingIdentifier }) {
                "Unknown rating level identifier: $ratingIdentifier"
            }
        }
    }

    private fun validateRatingLevels(ratingLevels: List<TaskRatingLevel>) {
        val ratingLevelIdentifiers = HashSet<String>(ratingLevels.size)

        for (ratingLevel in ratingLevels) {
            require(ratingLevel.identifier.isNotBlank()) { "Rating level identifier must not be blank" }
            require(ratingLevel.reaction.isNotBlank()) { "Rating level reaction must not be blank" }
            require(ratingLevelIdentifiers.add(ratingLevel.identifier)) {
                "Duplicate rating level identifier: ${ratingLevel.identifier}"
            }
        }
    }
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/service/TasksManager.kt` — modified

Delegates `replaceRatingLevels`; `@Inject` moved to the class (Metro warns on constructor-level).

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult

@SingleIn(TasksSessionScope::class)
class TasksManager @Inject constructor(private val tasksRepository: TasksRepository) {

    suspend fun createTasks(
        taskCreationRequests: List<TaskCreationRequest>
    ): TasksMutationResult = tasksRepository.createTasks(taskCreationRequests)

    suspend fun deleteTasks(
        taskIdentifiers: List<String>
    ): TasksMutationResult = tasksRepository.deleteTasks(taskIdentifiers)

    suspend fun updateTasks(
        taskUpdateRequests: List<TaskUpdateRequest>
    ): TasksMutationResult = tasksRepository.updateTasks(taskUpdateRequests)
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult

@Inject
@SingleIn(TasksSessionScope::class)
class TasksManager(private val tasksRepository: TasksRepository) {

    suspend fun createTasks(
        taskCreationRequests: List<TaskCreationRequest>
    ): TasksMutationResult = tasksRepository.createTasks(taskCreationRequests)

    suspend fun deleteTasks(
        taskIdentifiers: List<String>
    ): TasksMutationResult = tasksRepository.deleteTasks(taskIdentifiers)

    suspend fun updateTasks(
        taskUpdateRequests: List<TaskUpdateRequest>
    ): TasksMutationResult = tasksRepository.updateTasks(taskUpdateRequests)

    suspend fun replaceRatingLevels(
        ratingLevels: List<TaskRatingLevel>
    ): TasksMutationResult = tasksRepository.replaceRatingLevels(ratingLevels)
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/di/TasksBindings.kt` — new

Metro does not allow one graph to extend another (`may not directly extend graph class … Use @GraphExtension`), so the two providers move into a binding container that both `TasksSessionGraph` and the new `TasksScreenGraph` include — no duplicated providers.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksRepository

@BindingContainer
object TasksBindings {

    @Provides
    private fun provideTasksRepository(tasksContext: TasksContext): TasksRepository = tasksContext

    @Provides
    @SingleIn(TasksSessionScope::class)
    private fun provideTasksContext(): TasksContext = TasksContext()
}
```

#### `shared/core/feature/tasks/src/commonMain/kotlin/org/orev/nahidka/feature/tasks/di/TasksSessionGraph.kt` — modified

Uses `TasksBindings`.

**Old:**

```kotlin
package org.orev.nahidka.feature.tasks.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.service.TasksRepository

@DependencyGraph(TasksSessionScope::class)
interface TasksSessionGraph {
    val tasksContext: TasksContext
    val tasksManager: TasksManager

    companion object {
        @Provides
        private fun provideTasksRepository(context: TasksContext): TasksRepository = context

        @Provides
        @SingleIn(TasksSessionScope::class)
        private fun provideTasksContext(): TasksContext = TasksContext()
    }
}
```

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.di

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class])
interface TasksSessionGraph {
    val tasksContext: TasksContext
    val tasksManager: TasksManager
}
```

#### `shared/core/feature/tasks/src/commonTest/kotlin/org/orev/nahidka/feature/tasks/service/TasksContextTest.kt` — new

Core rating and due-date rules.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import kotlin.test.*

class TasksContextTest {

    private val excellentRatingLevel = TaskRatingLevel("excellent", "🤩")

    private val tasksContext = TasksContext(
        initialTasks = listOf(
            TaskRecord(
                identifier = "ship",
                title = "Ship feature",
                status = TaskStatus.DONE,
                ratingIdentifier = excellentRatingLevel.identifier
            )
        ),
        initialRatingLevels = listOf(TaskRatingLevel("bad", "😞"), excellentRatingLevel)
    )

    @Test
    fun leavingDoneStatusClearsRating() = runTest {
        tasksContext.updateTasks(listOf(TaskUpdateRequest("ship", status = TaskStatus.IN_PROGRESS)))

        assertNull(tasksContext.currentSnapshot().tasks.single().ratingIdentifier)
    }

    @Test
    fun ratingTaskThatIsNotDoneIsRejected() = runTest {
        tasksContext.createTasks(listOf(TaskCreationRequest("read-docs", "Read docs")))

        assertFailsWith<IllegalArgumentException> {
            tasksContext.updateTasks(
                listOf(
                    TaskUpdateRequest(
                        identifier = "read-docs",
                        ratingIdentifierPatch = NullablePatch.Set(excellentRatingLevel.identifier)
                    )
                )
            )
        }
    }

    @Test
    fun removingRatingLevelClearsItFromRatedTasks() = runTest {
        val mutationResult = tasksContext.replaceRatingLevels(listOf(TaskRatingLevel("bad", "😞")))

        assertTrue(mutationResult.changed)
        assertEquals(listOf("ship"), mutationResult.affectedTasks.map(TaskRecord::identifier))
        assertNull(tasksContext.currentSnapshot().tasks.single().ratingIdentifier)
    }

    @Test
    fun changingReactionAdvancesRevisionWithoutAffectingTasks() = runTest {
        val mutationResult = tasksContext.replaceRatingLevels(
            listOf(TaskRatingLevel("bad", "😞"), excellentRatingLevel.copy(reaction = "🔥"))
        )

        assertTrue(mutationResult.changed)
        assertTrue(mutationResult.affectedTasks.isEmpty())
        assertEquals(1L, tasksContext.currentSnapshot().revision)
        assertEquals("🔥", tasksContext.currentSnapshot().ratingLevels.last().reaction)
    }

    @Test
    fun dueDatePatchSetsAndClearsDueDate() = runTest {
        val dueDate = LocalDate(2026, 10, 31)

        tasksContext.updateTasks(listOf(TaskUpdateRequest("ship", dueDatePatch = NullablePatch.Set(dueDate))))
        assertEquals(dueDate, tasksContext.currentSnapshot().tasks.single().dueDate)

        tasksContext.updateTasks(listOf(TaskUpdateRequest("ship", dueDatePatch = NullablePatch.Clear)))
        assertNull(tasksContext.currentSnapshot().tasks.single().dueDate)
    }
}
```

#### `shared/core/feature/tasks/README.md` — modified

Fixes the deleted `api.TaskStatus` import; documents due dates, ratings and `TasksBindings`.

_Change 1 of 4 (old line 1 / new line 1)_

**Old:**

````markdown
# Tasks

In-memory task CRUD with atomic batches and immutable snapshots. Each context owns an independent session; callers supply identifiers and persistence.

```kotlin
````

**New:**

````markdown
# Tasks

In-memory task CRUD with atomic batches and immutable snapshots, optional due dates, and customizable reaction ratings for done tasks. Each context owns an independent session; callers supply identifiers and persistence.

```kotlin
````

_Change 2 of 4 (old line 13 / new line 13)_

**Old:**

````markdown

```kotlin
import org.orev.nahidka.api.TaskStatus
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
````

**New:**

````markdown

```kotlin
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
````

_Change 3 of 4 (old line 44 / new line 43)_

**Old:**

````markdown
    check(context.currentSnapshot().tasks.isEmpty())
}
```
````

**New:**

````markdown
    check(context.currentSnapshot().tasks.isEmpty())
}
```

**Due dates and ratings** — a rating is one of the customizable reactions; only `TaskStatus.acceptsRating` statuses (`DONE`) can hold one:

```kotlin
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import org.orev.nahidka.feature.tasks.service.TasksContext

suspend fun ratingExample() {
    val context = TasksContext() // Starts with DEFAULT_TASK_RATING_LEVELS: 😞 bad, 😐 okay, 🙂 good, 🤩 excellent.
    context.createTasks(listOf(TaskCreationRequest("ship", "Ship feature", dueDate = LocalDate(2026, 10, 31))))
    context.updateTasks(listOf(
        TaskUpdateRequest("ship", status = TaskStatus.DONE, ratingIdentifierPatch = NullablePatch.Set("excellent")),
    ))

    context.updateTasks(listOf(TaskUpdateRequest("ship", status = TaskStatus.IN_PROGRESS)))
    check(context.currentSnapshot().tasks.single().ratingIdentifier == null) // Leaving DONE clears the rating.

    val replaced = context.replaceRatingLevels(listOf(
        TaskRatingLevel("bad", "👎"),
        TaskRatingLevel("excellent", "🔥"),
    ))
    check(replaced.changed && replaced.affectedTasks.isEmpty()) // Level-only change still advances the revision.
}
// Rating a task that is not DONE, or with an unknown level identifier, throws IllegalArgumentException.
// replaceRatingLevels clears ratings that point at removed levels in the same revision;
// those tasks are returned as affectedTasks. Level order is the scale order, worst first.
```
````

_Change 4 of 4 (old line 78 / new line 106)_

**Old:**

````markdown

fun tasksSession(): TasksSessionGraph = createGraph<TasksSessionGraph>()
// Graph exposes tasksContext + tasksManager; TasksRepository binds to tasksContext.
// TasksSessionScope scopes one default-empty context and manager per graph.
// Retain the graph for the session. Use direct construction to seed initialTasks.
// There is no close()/dispose() API; the caller owns collector cancellation.
// App.kt uses remember(session) { TasksContext() } and remembers its manager.
// Recreating the context starts with the supplied initialTasks at revision 0.
```
````

**New:**

````markdown

fun tasksSession(): TasksSessionGraph = createGraph<TasksSessionGraph>()
// Graph exposes tasksContext + tasksManager; TasksBindings binds TasksRepository to tasksContext.
// Other graphs (for example TasksScreenGraph in :shared:ui:tasks) reuse TasksBindings.
// TasksSessionScope scopes one default-empty context and manager per graph.
// Retain the graph for the session. Use direct construction to seed initialTasks.
// There is no close()/dispose() API; the caller owns collector cancellation.
// App.kt remembers a TasksScreenGraph per session and takes TasksViewModel from it.
// Recreating the context starts with the supplied initialTasks at revision 0.
```
````

### Step 2 — New module `shared/ui/common`

Feature-agnostic UI building blocks (your choice in chat). The financial plan's `FinancialDialogController`, `FinancialDialogState`, `FinancialDialog`, `FinancialChoiceSelector`, `FinancialLayoutWidth`, `FinancialAddButton`, `FinancialDateField`, `FINANCIAL_DAY_FORMAT`, `FinancialTable` and the ⋮ menu become imports of these (see §5).

#### `settings.gradle.kts` — modified

Registers the module.

_Change 1 of 1 (old line 43 / new line 43)_

**Old:**

```kotlin
include(":shared:core:feature:dashboard")
include(":webApp")
include(":shared:ui:tasks")
include(":shared:ui:goal")
```

**New:**

```kotlin
include(":shared:core:feature:dashboard")
include(":webApp")
include(":shared:ui:common")
include(":shared:ui:tasks")
include(":shared:ui:goal")
```

#### `shared/ui/common/build.gradle.kts` — new

Copy of `shared/ui/template_build.gradle.kts` + datetime and lifecycle-runtime-compose.

**Old:** _(file does not exist)_

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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/layout/LayoutWidth.kt` — new

The single breakpoint (600 dp, Material “compact”).

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/ChoiceSelector.kt` — new

Generic segmented selector.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun <Option> ChoiceSelector(
    options: List<Option>,
    selectedOption: Option,
    optionTitle: @Composable (Option) -> String,
    onOptionSelect: (Option) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { optionIndex, option ->
            SegmentedButton(
                selected = option == selectedOption,
                onClick = { onOptionSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(optionIndex, options.size),
            ) {
                Text(
                    text = optionTitle(option),
                    modifier = Modifier.width(IntrinsicSize.Max),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/ChoiceField.kt` — new

Generic read-only dropdown field (status and rating in the task editor).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Option> ChoiceField(
    title: String,
    options: List<Option>,
    selectedOption: Option,
    optionTitle: @Composable (Option) -> String,
    onOptionSelect: (Option) -> Unit,
) {
    var optionsExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = optionsExpanded, onExpandedChange = { expanded -> optionsExpanded = expanded }) {
        OutlinedTextField(
            value = optionTitle(selectedOption),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = optionsExpanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = optionsExpanded, onDismissRequest = { optionsExpanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionTitle(option)) },
                    onClick = {
                        onOptionSelect(option)
                        optionsExpanded = false
                    },
                )
            }
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/AddButton.kt` — new

Text button when expanded, `＋` icon button when compact.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.orev.nahidka.ui.common.layout.LayoutWidth

@Composable
fun AddButton(
    title: String,
    layoutWidth: LayoutWidth,
    onClick: () -> Unit,
) {
    when (layoutWidth) {
        LayoutWidth.COMPACT -> FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = title },
        ) {
            Text("＋")
        }

        LayoutWidth.EXPANDED -> Button(onClick = onClick) {
            Text(title)
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/MenuAction.kt` — new

One menu entry.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

class MenuAction(
    val title: String,
    val onSelect: () -> Unit,
)
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/DropdownPopup.kt` — new

A trigger plus a dropdown with any content; owns the open/close state once for every popup.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*

@Composable
fun DropdownPopup(
    trigger: @Composable (onPopupOpen: () -> Unit) -> Unit,
    content: @Composable ColumnScope.(onPopupClose: () -> Unit) -> Unit,
) {
    var popupExpanded by remember { mutableStateOf(false) }

    Box {
        trigger { popupExpanded = true }
        DropdownMenu(expanded = popupExpanded, onDismissRequest = { popupExpanded = false }) {
            content { popupExpanded = false }
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/ActionsMenu.kt` — new

A list of actions in a `DropdownPopup`, with any trigger (status badge, ⋮).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ActionsMenu(
    menuActions: List<MenuAction>,
    trigger: @Composable (onMenuOpen: () -> Unit) -> Unit,
) {
    DropdownPopup(trigger) { onPopupClose ->
        menuActions.forEach { menuAction ->
            DropdownMenuItem(
                text = { Text(menuAction.title) },
                onClick = {
                    onPopupClose()
                    menuAction.onSelect()
                },
            )
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/MoreActionsMenu.kt` — new

The ⋮ trigger, with an accessible label.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoreActionsMenu(menuActions: List<MenuAction>) {
    val moreActionsTitle = stringResource(Res.string.common_action_more)

    ActionsMenu(menuActions) { onMenuOpen ->
        IconButton(onClick = onMenuOpen, modifier = Modifier.semantics { contentDescription = moreActionsTitle }) {
            Text("⋮")
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/DateField.kt` — new

Read-only field + Material date picker; optional ✕ clear (due date is optional; finance dates are not).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.datetime.*
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_cancel
import nahidka.shared.ui.common.generated.resources.common_action_clear
import nahidka.shared.ui.common.generated.resources.common_action_select
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.format.DAY_FORMAT
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate?,
    title: String,
    onDateSelect: (LocalDate) -> Unit,
    onDateClear: (() -> Unit)? = null,
) {
    var datePickerVisible by remember { mutableStateOf(false) }
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val clearTitle = stringResource(Res.string.common_action_clear)

    LaunchedEffect(fieldInteractionSource) {
        fieldInteractionSource.interactions
            .filterIsInstance<PressInteraction.Release>()
            .collect { datePickerVisible = true }
    }

    OutlinedTextField(
        value = date
            ?.format(DAY_FORMAT)
            .orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text(title) },
        leadingIcon = { Text("📅") },
        trailingIcon = if (date != null && onDateClear != null) {
            {
                IconButton(onClick = onDateClear, modifier = Modifier.semantics { contentDescription = clearTitle }) {
                    Text("✕")
                }
            }
        } else {
            null
        },
        singleLine = true,
        interactionSource = fieldInteractionSource,
    )

    if (datePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                ?.atStartOfDayIn(TimeZone.UTC)
                ?.toEpochMilliseconds(),
        )

        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMilliseconds ->
                            onDateSelect(
                                Instant
                                    .fromEpochMilliseconds(selectedDateMilliseconds)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date,
                            )
                        }
                        datePickerVisible = false
                    },
                ) {
                    Text(stringResource(Res.string.common_action_select))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(stringResource(Res.string.common_action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/component/DataTable.kt` — new

Bordered lazy table with an optional sticky header and an empty-state message.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val DATA_TABLE_HEADER_KEY = "data-table-header"

@Composable
fun <Row> DataTable(
    rows: List<Row>,
    rowKey: (Row) -> Any,
    emptyTableMessage: String,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    rowContent: @Composable (Row) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        if (rows.isEmpty()) {
            Text(
                text = emptyTableMessage,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                header?.let { tableHeader ->
                    stickyHeader(key = DATA_TABLE_HEADER_KEY) {
                        tableHeader()
                        HorizontalDivider()
                    }
                }
                items(rows, key = rowKey) { row ->
                    rowContent(row)
                    HorizontalDivider()
                }
            }
        }
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/format/DayFormat.kt` — new

`05.10.2026`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.char

val DAY_FORMAT = LocalDate.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/DialogState.kt` — new

Immutable dialog state.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

data class DialogState<Draft, Rejection : Any>(
    val draft: Draft,
    val submittable: Boolean,
    val submitting: Boolean = false,
    val rejection: Rejection? = null,
)
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/DialogController.kt` — new

One dialog lifecycle for every dialog: validation gate, double-submit guard, rejection display. `Rejection` is generic, so finance can keep `FinancialError` and tasks use `IllegalArgumentException`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialogController<Draft, Rejection : Any>(
    private val coroutineScope: CoroutineScope,
    private val draftValidation: (Draft) -> Boolean = { true },
    private val submission: suspend (Draft) -> Rejection?,
) {

    private val mutableDialogState = MutableStateFlow<DialogState<Draft, Rejection>?>(null)

    val dialogState: StateFlow<DialogState<Draft, Rejection>?> = mutableDialogState.asStateFlow()

    fun open(draft: Draft) {
        mutableDialogState.value = DialogState(draft, draftValidation(draft))
    }

    fun edit(draftTransformation: (Draft) -> Draft) {
        mutableDialogState.update { openedDialogState ->
            openedDialogState?.let { dialogState ->
                val editedDraft = draftTransformation(dialogState.draft)

                dialogState.copy(draft = editedDraft, submittable = draftValidation(editedDraft), rejection = null)
            }
        }
    }

    fun submit() {
        val submittedDialogState = mutableDialogState.value

        if (submittedDialogState == null || !submittedDialogState.submittable || submittedDialogState.submitting) {
            return
        }

        mutableDialogState.value = submittedDialogState.copy(submitting = true, rejection = null)

        coroutineScope.launch {
            val rejection = submission(submittedDialogState.draft)

            if (rejection == null) {
                dismiss()
            } else {
                mutableDialogState.update { openedDialogState ->
                    openedDialogState?.copy(submitting = false, rejection = rejection)
                }
            }
        }
    }

    fun dismiss() {
        mutableDialogState.value = null
    }
}
```

#### `shared/ui/common/src/commonMain/kotlin/org/orev/nahidka/ui/common/dialog/ControlledDialog.kt` — new

AlertDialog bound to a `DialogController`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Draft, Rejection : Any> ControlledDialog(
    dialogController: DialogController<Draft, Rejection>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    rejectionText: @Composable (Rejection) -> String,
    content: @Composable (Draft) -> Unit,
) {
    val openedDialogState by dialogController.dialogState.collectAsStateWithLifecycle()
    val dialogState = openedDialogState ?: return

    AlertDialog(
        onDismissRequest = dialogController::dismiss,
        title = { Text(title(dialogState.draft)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content(dialogState.draft)
                dialogState.rejection?.let { rejection ->
                    Text(rejectionText(rejection), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = dialogController::submit,
                enabled = dialogState.submittable && !dialogState.submitting,
            ) {
                Text(confirmationTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = dialogController::dismiss) {
                Text(stringResource(Res.string.common_action_cancel))
            }
        },
    )
}
```

#### `shared/ui/common/src/commonMain/composeResources/values/strings.xml` — new

**Old:** _(file does not exist)_

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Cancel</string>
    <string name="common_action_select">Select</string>
    <string name="common_action_clear">Clear</string>
    <string name="common_action_more">More actions</string>
</resources>
```

#### `shared/ui/common/src/commonMain/composeResources/values-uk/strings.xml` — new

**Old:** _(file does not exist)_

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Скасувати</string>
    <string name="common_action_select">Вибрати</string>
    <string name="common_action_clear">Очистити</string>
    <string name="common_action_more">Інші дії</string>
</resources>
```

#### `shared/ui/common/src/commonMain/composeResources/values-ru/strings.xml` — new

**Old:** _(file does not exist)_

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="common_action_cancel">Отмена</string>
    <string name="common_action_select">Выбрать</string>
    <string name="common_action_clear">Очистить</string>
    <string name="common_action_more">Другие действия</string>
</resources>
```

#### `shared/ui/common/src/commonTest/kotlin/org/orev/nahidka/ui/common/dialog/DialogControllerTest.kt` — new

Success closes, rejection stays until the next edit, invalid drafts are never submitted.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DialogControllerTest {

    private val submittedDrafts = mutableListOf<String>()

    @Test
    fun successfulSubmissionClosesDialog() = runTest {
        val dialogController = dialogController(rejection = null)

        dialogController.open("Write plan")
        dialogController.submit()
        advanceUntilIdle()

        assertEquals(listOf("Write plan"), submittedDrafts)
        assertNull(dialogController.dialogState.value)
    }

    @Test
    fun rejectedSubmissionKeepsDialogOpenUntilNextEdit() = runTest {
        val dialogController = dialogController(rejection = "Rejected")

        dialogController.open("Write plan")
        dialogController.submit()
        advanceUntilIdle()
        assertEquals("Rejected", checkNotNull(dialogController.dialogState.value).rejection)

        dialogController.edit { draft -> "$draft again" }
        assertEquals(DialogState("Write plan again", submittable = true), dialogController.dialogState.value)
    }

    @Test
    fun invalidDraftIsNotSubmitted() = runTest {
        val dialogController = dialogController(rejection = null)

        dialogController.open(" ")
        dialogController.submit()
        advanceUntilIdle()

        assertTrue(submittedDrafts.isEmpty())
        assertFalse(checkNotNull(dialogController.dialogState.value).submittable)
    }

    private fun TestScope.dialogController(rejection: String?): DialogController<String, String> =
        DialogController(this, String::isNotBlank) { draft ->
            submittedDrafts.add(draft)
            rejection
        }
}
```

### Step 3 — Tasks UI: build, models, ViewModel, DI

#### `gradle/libs.versions.toml` — modified

`compose.desktop.uiTestJUnit4` is deprecated in Compose 1.11 (“Specify dependency directly”), so the test artifact gets a catalog entry.

_Change 1 of 1 (old line 51 / new line 51)_

**Old:**

```toml
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
compose-uiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }
kotlinx-coroutinesSwing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
```

**New:**

```toml
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
compose-uiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }
compose-uiTestJunit4 = { module = "org.jetbrains.compose.ui:ui-test-junit4", version.ref = "composeMultiplatform" }
kotlinx-coroutinesSwing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
```

#### `shared/ui/tasks/build.gradle.kts` — modified

Core, `ui:common`, lifecycle, Metro; desktop UI-test dependencies for `jvmTest`.

_Change 1 of 3 (old line 7 / new line 7)_

**Old:**

```kotlin
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
```

**New:**

```kotlin
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}
```

_Change 2 of 3 (old line 23 / new line 24)_

**Old:**

```kotlin
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
```

**New:**

```kotlin
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:feature:tasks"))
            implementation(project(":shared:ui:common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
```

_Change 3 of 3 (old line 30 / new line 33)_

**Old:**

```kotlin
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
```

**New:**

```kotlin
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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/model/TaskItem.kt` — new

A task with its resolved rating level.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.model

import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskRecord

data class TaskItem(
    val task: TaskRecord,
    val ratingLevel: TaskRatingLevel?,
)
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/model/TasksContent.kt` — new

Snapshot → UI content (`taskItemsWithStatus` feeds board columns and tabs).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot

data class TasksContent(
    val taskItems: ImmutableList<TaskItem>,
    val ratingLevels: ImmutableList<TaskRatingLevel>,
) {

    fun taskItemsWithStatus(status: TaskStatus): List<TaskItem> =
        taskItems.filter { taskItem -> taskItem.task.status == status }

    companion object {

        fun of(tasksSnapshot: TasksSnapshot): TasksContent {
            val ratingLevelsByIdentifier = tasksSnapshot.ratingLevels.associateBy(TaskRatingLevel::identifier)

            return TasksContent(
                taskItems = tasksSnapshot.tasks
                    .map { task -> TaskItem(task, task.ratingIdentifier?.let(ratingLevelsByIdentifier::get)) }
                    .toPersistentList(),
                ratingLevels = tasksSnapshot.ratingLevels,
            )
        }
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/model/TaskDraft.kt` — new

Editor draft; builds core requests with `nullablePatch` from `core:common`, so only changed fields are patched.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.model

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.nullablePatch
import org.orev.nahidka.feature.tasks.dto.*

data class TaskDraft(
    val editedTask: TaskRecord?,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val dueDate: LocalDate?,
    val ratingLevel: TaskRatingLevel?,
) {

    val submittable: Boolean
        get() = title.isNotBlank()

    fun withStatus(status: TaskStatus): TaskDraft =
        copy(status = status, ratingLevel = ratingLevel.takeIf { status.acceptsRating })

    fun toCreationRequest(identifier: String): TaskCreationRequest = TaskCreationRequest(
        identifier = identifier,
        title = title.trim(),
        description = description.trim(),
        status = status,
        dueDate = dueDate,
        ratingIdentifier = ratingLevel?.identifier,
    )

    fun toUpdateRequest(editedTask: TaskRecord): TaskUpdateRequest = TaskUpdateRequest(
        identifier = editedTask.identifier,
        title = title.trim(),
        descriptionPatch = nullablePatch(editedTask.description, description.trim()),
        status = status,
        dueDatePatch = nullablePatch(editedTask.dueDate, dueDate),
        ratingIdentifierPatch = nullablePatch(editedTask.ratingIdentifier, ratingLevel?.identifier),
    )

    companion object {

        fun creation(): TaskDraft = TaskDraft(
            editedTask = null,
            title = "",
            description = "",
            status = TaskStatus.TO_DO,
            dueDate = null,
            ratingLevel = null,
        )

        fun editing(taskItem: TaskItem): TaskDraft = TaskDraft(
            editedTask = taskItem.task,
            title = taskItem.task.title,
            description = taskItem.task.description,
            status = taskItem.task.status,
            dueDate = taskItem.task.dueDate,
            ratingLevel = taskItem.ratingLevel,
        )
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/model/TaskRatingLevelsDraft.kt` — new

Rating-levels editor draft: add, rename, reorder, remove.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.model

import kotlinx.collections.immutable.PersistentList
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel

data class TaskRatingLevelsDraft(val ratingLevels: PersistentList<TaskRatingLevel>) {

    val submittable: Boolean
        get() = ratingLevels.all { ratingLevel -> ratingLevel.reaction.isNotBlank() }

    fun adding(ratingLevel: TaskRatingLevel): TaskRatingLevelsDraft =
        copy(ratingLevels = ratingLevels.adding(ratingLevel))

    fun replacingReaction(ratingLevelIndex: Int, reaction: String): TaskRatingLevelsDraft =
        copy(
            ratingLevels = ratingLevels.replacingAt(
                ratingLevelIndex,
                ratingLevels[ratingLevelIndex].copy(reaction = reaction),
            ),
        )

    fun moving(ratingLevelIndex: Int, targetIndex: Int): TaskRatingLevelsDraft =
        copy(
            ratingLevels = ratingLevels
                .removingAt(ratingLevelIndex)
                .addingAt(targetIndex, ratingLevels[ratingLevelIndex]),
        )

    fun removing(ratingLevelIndex: Int): TaskRatingLevelsDraft =
        copy(ratingLevels = ratingLevels.removingAt(ratingLevelIndex))
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/model/TaskInteractions.kt` — new

Edit / delete / move / rate callbacks shared by cards, rows and menus (no ViewModel inside leaf composables).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.model

import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus

class TaskInteractions(
    val onTaskEdit: (TaskItem) -> Unit,
    val onTaskDelete: (TaskItem) -> Unit,
    val onTaskMove: (TaskItem, TaskStatus) -> Unit,
    val onTaskRate: (TaskItem, TaskRatingLevel?) -> Unit,
)
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskDialogController.kt` — new

Tasks reject with the core’s `IllegalArgumentException`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import org.orev.nahidka.ui.common.dialog.DialogController

typealias TaskDialogController<Draft> = DialogController<Draft, IllegalArgumentException>
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksViewModel.kt` — new

Observes `TasksRepository.tasksState`, mutates through `TasksManager`. Quick changes (drag & drop, “Move to”, reaction rating) report failures through `changeRejections` (snackbar) instead of crashing.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.nullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.service.TasksRepository
import org.orev.nahidka.ui.tasks.dialog.TaskDialogController
import org.orev.nahidka.ui.tasks.model.*

private const val TASKS_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS = 5_000L

@Inject
class TasksViewModel(
    private val tasksRepository: TasksRepository,
    private val tasksManager: TasksManager,
    private val identifierGenerator: IdentifierGenerator,
) : ViewModel() {

    private val mutableChangeRejections = MutableSharedFlow<IllegalArgumentException>(extraBufferCapacity = 1)

    val tasksContent: StateFlow<TasksContent> = tasksRepository.tasksState
        .map(TasksContent::of)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TASKS_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS),
            initialValue = TasksContent.of(tasksRepository.tasksState.value),
        )

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

    val taskInteractions = TaskInteractions(
        onTaskEdit = ::openTaskEditing,
        onTaskDelete = taskDeletion::open,
        onTaskMove = ::moveTask,
        onTaskRate = ::rateTask,
    )

    fun openTaskCreation() {
        taskEditor.open(TaskDraft.creation())
    }

    fun openTaskEditing(taskItem: TaskItem) {
        taskEditor.open(TaskDraft.editing(taskItem))
    }

    fun openRatingLevelsEditing() {
        val ratingLevels = tasksRepository.tasksState.value.ratingLevels

        ratingLevelsEditor.open(TaskRatingLevelsDraft(ratingLevels.toPersistentList()))
    }

    fun addRatingLevel() {
        ratingLevelsEditor.edit { ratingLevelsDraft ->
            ratingLevelsDraft.adding(TaskRatingLevel(identifierGenerator.next(), ""))
        }
    }

    fun moveTask(taskItem: TaskItem, status: TaskStatus) {
        changeTask(TaskUpdateRequest(taskItem.task.identifier, status = status))
    }

    fun rateTask(taskItem: TaskItem, ratingLevel: TaskRatingLevel?) {
        changeTask(
            TaskUpdateRequest(
                identifier = taskItem.task.identifier,
                ratingIdentifierPatch = nullablePatch(taskItem.task.ratingIdentifier, ratingLevel?.identifier),
            ),
        )
    }

    private fun changeTask(taskUpdateRequest: TaskUpdateRequest) {
        viewModelScope.launch {
            rejectionOf { tasksManager.updateTasks(listOf(taskUpdateRequest)) }
                ?.let(mutableChangeRejections::tryEmit)
        }
    }

    private suspend fun saveTask(taskDraft: TaskDraft): TasksMutationResult {
        val editedTask = taskDraft.editedTask

        return if (editedTask == null) {
            tasksManager.createTasks(listOf(taskDraft.toCreationRequest(identifierGenerator.next())))
        } else {
            tasksManager.updateTasks(listOf(taskDraft.toUpdateRequest(editedTask)))
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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksScreenGraph.kt` — new

Composition root used by the host **and** by the tests (the tests verify the real wiring).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.feature.tasks.di.TasksBindings
import org.orev.nahidka.feature.tasks.di.TasksSessionScope

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class])
interface TasksScreenGraph {
    val tasksViewModel: TasksViewModel

    @Provides
    fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
```

### Step 4 — String resources (en / uk / ru)

Ukrainian uses “завдання” to match the existing `feature_task_title`.

#### `shared/ui/tasks/src/commonMain/composeResources/values/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Tasks</string>
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

#### `shared/ui/tasks/src/commonMain/composeResources/values-uk/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Завдання</string>
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

#### `shared/ui/tasks/src/commonMain/composeResources/values-ru/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_task_title">Задачи</string>
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

### Step 5 — Tasks UI: shared components (`component/`)

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskStatusTitle.kt` — new

Status → string; “To Do (3)” for column headers and phone tabs.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskStatus

internal val TaskStatus.title: StringResource
    get() = when (this) {
        TaskStatus.TO_DO -> Res.string.tasks_status_to_do
        TaskStatus.IN_PROGRESS -> Res.string.tasks_status_in_progress
        TaskStatus.DONE -> Res.string.tasks_status_done
    }

@Composable
internal fun taskStatusTitleWithCount(status: TaskStatus, taskCount: Int): String =
    stringResource(Res.string.tasks_status_count, stringResource(status.title), taskCount)
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskStatusBadge.kt` — new

Jira-style lozenge, theme colors only (works in dark mode).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskStatus

@Composable
internal fun TaskStatusBadge(status: TaskStatus, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = status.containerColor,
        contentColor = status.contentColor,
    ) {
        Text(
            text = stringResource(status.title)
                .uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val TaskStatus.containerColor: Color
    @Composable
    get() = when (this) {
        TaskStatus.TO_DO -> MaterialTheme.colorScheme.surfaceVariant
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
        TaskStatus.DONE -> MaterialTheme.colorScheme.tertiaryContainer
    }

private val TaskStatus.contentColor: Color
    @Composable
    get() = when (this) {
        TaskStatus.TO_DO -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
        TaskStatus.DONE -> MaterialTheme.colorScheme.onTertiaryContainer
    }
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskMenuActions.kt` — new

The edit/delete and “Move to …” menu entries, built once for cards, rows and badges.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
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
internal fun taskStatusChangeActions(taskItem: TaskItem, taskInteractions: TaskInteractions): List<MenuAction> =
    TaskStatus.entries
        .filter { status -> status != taskItem.task.status }
        .map { status ->
            MenuAction(stringResource(Res.string.tasks_action_move_to, stringResource(status.title))) {
                taskInteractions.onTaskMove(taskItem, status)
            }
        }
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskStatusMenu.kt` — new

Status badge that opens “Move to …”.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import org.orev.nahidka.ui.common.component.ActionsMenu
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskStatusMenu(taskItem: TaskItem, taskInteractions: TaskInteractions) {
    ActionsMenu(taskStatusChangeActions(taskItem, taskInteractions)) { onMenuOpen ->
        TaskStatusBadge(taskItem.task.status, onClick = onMenuOpen)
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskRatingReactions.kt` — new

Reaction bar: one chip per reaction, with the chosen one highlighted; tapping it again removes the rating. The rating popup and the task editor both use it.

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskRatingMenu.kt` — new

Done tasks: shows the chosen reaction (or “Rate”) and opens the reaction bar in a `DropdownPopup`. Cards and list rows both use it.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_rating_rate
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.DropdownPopup
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskRatingMenu(
    taskItem: TaskItem,
    ratingLevels: List<TaskRatingLevel>,
    taskInteractions: TaskInteractions,
) {
    if (!taskItem.task.status.acceptsRating || ratingLevels.isEmpty()) {
        return
    }

    DropdownPopup(
        trigger = { onPopupOpen ->
            TextButton(onClick = onPopupOpen) {
                Text(
                    text = taskItem.ratingLevel?.reaction ?: stringResource(Res.string.tasks_rating_rate),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { onPopupClose ->
        TaskRatingReactions(
            ratingLevels = ratingLevels,
            selectedRatingLevel = taskItem.ratingLevel,
            onRatingLevelSelect = { ratingLevel ->
                onPopupClose()
                taskInteractions.onTaskRate(taskItem, ratingLevel)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/component/TaskDueDateText.kt` — new

`📅 05.10.2026`.

**Old:** _(file does not exist)_

**New:**

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

### Step 6 — Board (`board/`)

Drag & drop is plain common Compose code (no platform `DragAndDrop` APIs, so it behaves the same on Android, iOS, desktop and web): a mouse drags immediately, touch drags after a long press, so scrolling a column with a finger still works.

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskCard.kt` — new

Card from the mockup: title, description cut with `…` after 3 lines, due date; Done cards add their reaction; ⋮ menu.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.tasks.component.TaskDueDateText
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.taskEditingActions
import org.orev.nahidka.ui.tasks.component.taskStatusChangeActions
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

private const val TASK_CARD_DESCRIPTION_MAXIMUM_LINES = 3

@Composable
internal fun TaskCard(
    taskItem: TaskItem,
    ratingLevels: List<TaskRatingLevel>,
    taskInteractions: TaskInteractions,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = { taskInteractions.onTaskEdit(taskItem) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = taskItem.task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MoreActionsMenu(
                    menuActions = taskStatusChangeActions(taskItem, taskInteractions) +
                        taskEditingActions(taskItem, taskInteractions),
                )
            }
            if (taskItem.task.description.isNotBlank()) {
                Text(
                    text = taskItem.task.description,
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = TASK_CARD_DESCRIPTION_MAXIMUM_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (taskItem.task.dueDate != null || taskItem.task.status.acceptsRating) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                    Spacer(Modifier.weight(1f))
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
                }
            }
        }
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskCardList.kt` — new

Lazy list of cards or “No tasks”.

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskBoardColumn.kt` — new

Column surface with header; highlighted while it is the drop target.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.component.taskStatusTitleWithCount

@Composable
internal fun TaskBoardColumn(
    status: TaskStatus,
    taskCount: Int,
    dropTarget: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (dropTarget) 2.dp else 1.dp,
            color = if (dropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(taskStatusTitleWithCount(status, taskCount), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskBoardDragState.kt` — new

Tracks the dragged card, pointer and column bounds; resolves the drop target.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem

@Stable
internal class TaskBoardDragState {

    private val columnBoundsByStatus = mutableMapOf<TaskStatus, Rect>()
    private val cardBoundsByTaskIdentifier = mutableMapOf<String, Rect>()
    private var boardPosition = Offset.Zero
    private var grabPosition = Offset.Zero
    private var dragDistance by mutableStateOf(Offset.Zero)

    var draggedTaskItem by mutableStateOf<TaskItem?>(null)
        private set

    var draggedCardBounds by mutableStateOf(Rect.Zero)
        private set

    val hoveredStatus: TaskStatus? by derivedStateOf {
        val pointerPosition = draggedCardBounds.topLeft + grabPosition + dragDistance

        columnBoundsByStatus.entries
            .firstOrNull { (_, columnBounds) -> columnBounds.contains(pointerPosition) }
            ?.key
            .takeIf { draggedTaskItem != null }
    }

    val draggedCardOffset: IntOffset
        get() = (draggedCardBounds.topLeft + dragDistance - boardPosition).round()

    fun isDragged(taskItem: TaskItem): Boolean =
        draggedTaskItem?.task?.identifier == taskItem.task.identifier

    fun placeBoard(boardPosition: Offset) {
        this.boardPosition = boardPosition
    }

    fun placeColumn(status: TaskStatus, columnBounds: Rect) {
        columnBoundsByStatus[status] = columnBounds
    }

    fun placeCard(taskIdentifier: String, cardBounds: Rect) {
        cardBoundsByTaskIdentifier[taskIdentifier] = cardBounds
    }

    fun start(taskItem: TaskItem, grabPosition: Offset) {
        this.grabPosition = grabPosition
        draggedCardBounds = cardBoundsByTaskIdentifier[taskItem.task.identifier] ?: Rect.Zero
        dragDistance = Offset.Zero
        draggedTaskItem = taskItem
    }

    fun drag(dragAmount: Offset) {
        dragDistance += dragAmount
    }

    fun drop(onTaskDrop: (TaskItem, TaskStatus) -> Unit) {
        val droppedTaskItem = draggedTaskItem
        val targetStatus = hoveredStatus

        if (droppedTaskItem != null && targetStatus != null && targetStatus != droppedTaskItem.task.status) {
            onTaskDrop(droppedTaskItem, targetStatus)
        }

        cancel()
    }

    fun cancel() {
        draggedTaskItem = null
        dragDistance = Offset.Zero
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TaskCardDragGesture.kt` — new

Mouse = drag after touch slop, touch/stylus = drag after long press.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem

internal fun Modifier.taskCardDrag(
    taskItem: TaskItem,
    taskBoardDragState: TaskBoardDragState,
    onTaskDrop: (TaskItem, TaskStatus) -> Unit,
): Modifier = this
    .onGloballyPositioned { cardCoordinates ->
        taskBoardDragState.placeCard(taskItem.task.identifier, cardCoordinates.boundsInRoot())
    }
    .pointerInput(taskItem) {
        awaitEachGesture {
            val pointerDown = awaitFirstDown(requireUnconsumed = false)
            val dragStart = if (pointerDown.type == PointerType.Mouse) {
                awaitTouchSlopOrCancellation(pointerDown.id) { pointerChange, _ -> pointerChange.consume() }
            } else {
                awaitLongPressOrCancellation(pointerDown.id)
            }

            if (dragStart != null) {
                taskBoardDragState.start(taskItem, pointerDown.position)
                taskBoardDragState.drag(dragStart.position - pointerDown.position)

                val dragCompleted = drag(dragStart.id) { pointerChange ->
                    taskBoardDragState.drag(pointerChange.positionChange())
                    pointerChange.consume()
                }

                if (dragCompleted) {
                    taskBoardDragState.drop(onTaskDrop)
                } else {
                    taskBoardDragState.cancel()
                }
            }
        }
    }
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/board/TasksBoard.kt` — new

Expanded: three columns + floating dragged card. Compact: scrollable status tabs + one column.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.layout.*
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.component.taskStatusTitleWithCount
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TasksContent

private val TASK_BOARD_MAXIMUM_WIDTH = 1200.dp
private const val DRAGGED_TASK_CARD_SOURCE_ALPHA = 0.4f
private const val DRAGGED_TASK_CARD_ROTATION_DEGREES = 2f

@Composable
internal fun TasksBoard(
    tasksContent: TasksContent,
    layoutWidth: LayoutWidth,
    taskInteractions: TaskInteractions,
) {
    when (layoutWidth) {
        LayoutWidth.COMPACT -> CompactTasksBoard(tasksContent, taskInteractions)
        LayoutWidth.EXPANDED -> ExpandedTasksBoard(tasksContent, taskInteractions)
    }
}

@Composable
private fun CompactTasksBoard(tasksContent: TasksContent, taskInteractions: TaskInteractions) {
    var selectedStatus by rememberSaveable { mutableStateOf(TaskStatus.TO_DO) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedStatus.ordinal, edgePadding = 0.dp) {
            TaskStatus.entries.forEach { status ->
                Tab(
                    selected = status == selectedStatus,
                    onClick = { selectedStatus = status },
                    text = { Text(taskStatusTitleWithCount(status, tasksContent.taskItemsWithStatus(status).size)) },
                )
            }
        }
        TaskCardList(tasksContent.taskItemsWithStatus(selectedStatus)) { taskItem ->
            TaskCard(taskItem, tasksContent.ratingLevels, taskInteractions)
        }
    }
}

@Composable
private fun ExpandedTasksBoard(tasksContent: TasksContent, taskInteractions: TaskInteractions) {
    val taskBoardDragState = remember { TaskBoardDragState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { boardCoordinates ->
                taskBoardDragState.placeBoard(boardCoordinates.positionInRoot())
            },
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = TASK_BOARD_MAXIMUM_WIDTH)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                val statusTaskItems = tasksContent.taskItemsWithStatus(status)

                TaskBoardColumn(
                    status = status,
                    taskCount = statusTaskItems.size,
                    dropTarget = taskBoardDragState.hoveredStatus == status,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { columnCoordinates ->
                            taskBoardDragState.placeColumn(status, columnCoordinates.boundsInRoot())
                        },
                ) {
                    TaskCardList(statusTaskItems) { taskItem ->
                        TaskCard(
                            taskItem = taskItem,
                            ratingLevels = tasksContent.ratingLevels,
                            taskInteractions = taskInteractions,
                            modifier = Modifier
                                .taskCardDrag(taskItem, taskBoardDragState, taskInteractions.onTaskMove)
                                .alpha(
                                    if (taskBoardDragState.isDragged(taskItem)) DRAGGED_TASK_CARD_SOURCE_ALPHA else 1f,
                                ),
                        )
                    }
                }
            }
        }
        taskBoardDragState.draggedTaskItem?.let { draggedTaskItem ->
            TaskCard(
                taskItem = draggedTaskItem,
                ratingLevels = tasksContent.ratingLevels,
                taskInteractions = taskInteractions,
                modifier = Modifier
                    .zIndex(1f)
                    .offset { taskBoardDragState.draggedCardOffset }
                    .width(with(LocalDensity.current) { taskBoardDragState.draggedCardBounds.width.toDp() })
                    .rotate(DRAGGED_TASK_CARD_ROTATION_DEGREES),
            )
        }
    }
}
```

### Step 7 — List view (`list/`)

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/list/TaskListColumn.kt` — new

Column titles and weights, shared by header and rows.

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/list/TaskListRow.kt` — new

Expanded: Title · Description `…` · Status · Due date · Rating · ⋮. Compact: two-line row, metadata wraps.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.component.TaskDueDateText
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.TaskStatusMenu
import org.orev.nahidka.ui.tasks.component.taskEditingActions
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskListRow(
    taskItem: TaskItem,
    ratingLevels: List<TaskRatingLevel>,
    layoutWidth: LayoutWidth,
    taskInteractions: TaskInteractions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { taskInteractions.onTaskEdit(taskItem) }
            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (layoutWidth) {
            LayoutWidth.COMPACT -> Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TaskTitleText(taskItem)
                if (taskItem.task.description.isNotBlank()) {
                    TaskDescriptionText(taskItem)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    TaskStatusMenu(taskItem, taskInteractions)
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
                }
            }

            LayoutWidth.EXPANDED -> {
                TaskTitleText(taskItem, Modifier.weight(TaskListColumn.TITLE.weight))
                TaskDescriptionText(taskItem, Modifier.weight(TaskListColumn.DESCRIPTION.weight))
                Box(Modifier.weight(TaskListColumn.STATUS.weight)) {
                    TaskStatusMenu(taskItem, taskInteractions)
                }
                Box(Modifier.weight(TaskListColumn.DUE_DATE.weight)) {
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                }
                Box(Modifier.weight(TaskListColumn.RATING.weight)) {
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
                }
            }
        }
        MoreActionsMenu(taskEditingActions(taskItem, taskInteractions))
    }
}

@Composable
private fun TaskTitleText(taskItem: TaskItem, modifier: Modifier = Modifier) {
    Text(
        text = taskItem.task.title,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TaskDescriptionText(taskItem: TaskItem, modifier: Modifier = Modifier) {
    Text(
        text = taskItem.task.description,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/list/TasksList.kt` — new

`DataTable` with a sticky header on wide screens.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_list_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DataTable
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TasksContent

private val TASK_LIST_ACTIONS_WIDTH = 48.dp

@Composable
internal fun TasksList(
    tasksContent: TasksContent,
    layoutWidth: LayoutWidth,
    taskInteractions: TaskInteractions,
) {
    DataTable(
        rows = tasksContent.taskItems,
        rowKey = { taskItem -> taskItem.task.identifier },
        emptyTableMessage = stringResource(Res.string.tasks_list_empty),
        header = if (layoutWidth == LayoutWidth.EXPANDED) {
            { TaskListHeader() }
        } else {
            null
        },
    ) { taskItem ->
        TaskListRow(taskItem, tasksContent.ratingLevels, layoutWidth, taskInteractions)
    }
}

@Composable
private fun TaskListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TaskListColumn.entries.forEach { taskListColumn ->
            Text(
                text = stringResource(taskListColumn.title),
                modifier = Modifier.weight(taskListColumn.weight),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(TASK_LIST_ACTIONS_WIDTH))
    }
}
```

### Step 8 — Dialogs (`dialog/`)

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskDialog.kt` — new

`ControlledDialog` with the tasks rejection text.

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskEditorDialog.kt` — new

Create / edit: title, description, status, due date, and the reaction bar (Done only).

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskDeletionDialog.kt` — new

Delete confirmation.

**Old:** _(file does not exist)_

**New:**

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

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/dialog/TaskRatingLevelsDialog.kt` — new

Customize reactions: edit inline, ⋮ move up / move down / delete, “＋ Add reaction”.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
            text = stringResource(Res.string.tasks_rating_levels_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ratingLevelsDraft.ratingLevels.forEachIndexed { ratingLevelIndex, ratingLevel ->
            key(ratingLevel.identifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ratingLevel.reaction,
                        onValueChange = { reaction ->
                            ratingLevelsEditor.edit { draft -> draft.replacingReaction(ratingLevelIndex, reaction) }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(Res.string.tasks_rating_level_title, ratingLevelIndex + 1)) },
                        singleLine = true,
                    )
                    MoreActionsMenu(ratingLevelActions(ratingLevelsDraft, ratingLevelIndex, ratingLevelsEditor::edit))
                }
            }
        }
        TextButton(onClick = onRatingLevelAdd) {
            Text(stringResource(Res.string.tasks_rating_level_add))
        }
    }
}

@Composable
private fun ratingLevelActions(
    ratingLevelsDraft: TaskRatingLevelsDraft,
    ratingLevelIndex: Int,
    onRatingLevelsDraftEdit: ((TaskRatingLevelsDraft) -> TaskRatingLevelsDraft) -> Unit,
): List<MenuAction> = buildList {
    if (ratingLevelIndex > 0) {
        add(
            MenuAction(stringResource(Res.string.tasks_action_move_up)) {
                onRatingLevelsDraftEdit { draft -> draft.moving(ratingLevelIndex, ratingLevelIndex - 1) }
            },
        )
    }
    if (ratingLevelIndex < ratingLevelsDraft.ratingLevels.lastIndex) {
        add(
            MenuAction(stringResource(Res.string.tasks_action_move_down)) {
                onRatingLevelsDraftEdit { draft -> draft.moving(ratingLevelIndex, ratingLevelIndex + 1) }
            },
        )
    }
    add(
        MenuAction(stringResource(Res.string.tasks_action_delete)) {
            onRatingLevelsDraftEdit { draft -> draft.removing(ratingLevelIndex) }
        },
    )
}
```

### Step 9 — Screen entry point

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksView.kt` — new

BOARD | LIST.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_view_board
import nahidka.shared.ui.tasks.generated.resources.tasks_view_list
import org.jetbrains.compose.resources.StringResource

internal enum class TasksView(val title: StringResource) {
    BOARD(Res.string.tasks_view_board),
    LIST(Res.string.tasks_view_list),
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksHeader.kt` — new

Mockup header: [Board | List] [Create new task] [⋮ → Rating reactions]; one row on every width.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_create
import nahidka.shared.ui.tasks.generated.resources.tasks_rating_levels
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.AddButton
import org.orev.nahidka.ui.common.component.ChoiceSelector
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.layout.LayoutWidth

@Composable
internal fun TasksHeader(
    selectedView: TasksView,
    layoutWidth: LayoutWidth,
    onViewSelect: (TasksView) -> Unit,
    onTaskCreate: () -> Unit,
    onRatingLevelsEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceSelector(
            options = TasksView.entries,
            selectedOption = selectedView,
            optionTitle = { tasksView -> stringResource(tasksView.title) },
            onOptionSelect = onViewSelect,
            modifier = if (layoutWidth == LayoutWidth.COMPACT) Modifier.weight(1f) else Modifier,
        )
        AddButton(stringResource(Res.string.tasks_create), layoutWidth, onTaskCreate)
        MoreActionsMenu(listOf(MenuAction(stringResource(Res.string.tasks_rating_levels), onRatingLevelsEdit)))
    }
}
```

#### `shared/ui/tasks/src/commonMain/kotlin/org/orev/nahidka/ui/tasks/TasksScreen.kt` — new

Public entry point: width class, header, board/list, dialogs, snackbar.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.board.TasksBoard
import org.orev.nahidka.ui.tasks.dialog.TaskDeletionDialog
import org.orev.nahidka.ui.tasks.dialog.TaskEditorDialog
import org.orev.nahidka.ui.tasks.dialog.TaskRatingLevelsDialog
import org.orev.nahidka.ui.tasks.list.TasksList

@Composable
fun TasksScreen(tasksViewModel: TasksViewModel, modifier: Modifier = Modifier) {
    val tasksContent by tasksViewModel.tasksContent.collectAsStateWithLifecycle()
    var selectedView by rememberSaveable { mutableStateOf(TasksView.BOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val changeRejectionMessage = stringResource(Res.string.tasks_error_unsaved)

    LaunchedEffect(tasksViewModel, changeRejectionMessage) {
        tasksViewModel.changeRejections.collect { snackbarHostState.showSnackbar(changeRejectionMessage) }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val layoutWidth = LayoutWidth.of(maxWidth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (layoutWidth == LayoutWidth.COMPACT) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TasksHeader(
                selectedView = selectedView,
                layoutWidth = layoutWidth,
                onViewSelect = { tasksView -> selectedView = tasksView },
                onTaskCreate = tasksViewModel::openTaskCreation,
                onRatingLevelsEdit = tasksViewModel::openRatingLevelsEditing,
            )
            when (selectedView) {
                TasksView.BOARD -> TasksBoard(tasksContent, layoutWidth, tasksViewModel.taskInteractions)
                TasksView.LIST -> TasksList(tasksContent, layoutWidth, tasksViewModel.taskInteractions)
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    TaskEditorDialog(tasksViewModel.taskEditor, tasksContent.ratingLevels)
    TaskDeletionDialog(tasksViewModel.taskDeletion)
    TaskRatingLevelsDialog(tasksViewModel.ratingLevelsEditor, tasksViewModel::addRatingLevel)
}
```

### Step 10 — Tests (`src/jvmTest`)

UI tests drive the real `TasksScreen` with real pointer input: mouse drag, touch long-press drag, compact ⋮ menu, rating with a reaction and removing it, click-to-edit.

#### `shared/ui/tasks/src/jvmTest/kotlin/org/orev/nahidka/ui/tasks/TasksTest.kt` — new

Shared base: Main dispatcher, ViewModel from the real `TasksScreenGraph`, `createTask`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class TasksTest {

    protected val tasksViewModel by lazy { createGraph<TasksScreenGraph>().tasksViewModel }

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun createTask(title: String, status: TaskStatus = TaskStatus.TO_DO) {
        tasksViewModel.openTaskCreation()
        tasksViewModel.taskEditor.edit { taskDraft ->
            taskDraft
                .copy(title = title)
                .withStatus(status)
        }
        tasksViewModel.taskEditor.submit()
    }
}
```

#### `shared/ui/tasks/src/jvmTest/kotlin/org/orev/nahidka/ui/tasks/TasksViewModelTest.kt` — new

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem
import org.orev.nahidka.ui.tasks.model.TasksContent
import kotlin.test.*

class TasksViewModelTest : TasksTest() {

    @Test
    fun taskCreationAddsTaskToToDoColumn() = runTest {
        createTask("Write plan")

        val createdTaskItem = awaitContent { tasksContent -> tasksContent.taskItems.isNotEmpty() }
            .taskItemsWithStatus(TaskStatus.TO_DO)
            .single()

        assertEquals("Write plan", createdTaskItem.task.title)
        assertNull(tasksViewModel.taskEditor.dialogState.value)
    }

    @Test
    fun taskDraftWithBlankTitleIsNotSubmittable() {
        tasksViewModel.openTaskCreation()
        tasksViewModel.taskEditor.edit { taskDraft -> taskDraft.copy(title = "  ") }

        assertFalse(checkNotNull(tasksViewModel.taskEditor.dialogState.value).submittable)
    }

    @Test
    fun taskEditingSavesDueDateAndKeepsRating() = runTest {
        val dueDate = LocalDate(2026, 10, 31)
        val createdTaskItem = createDoneRatedTask("Ship feature")

        tasksViewModel.openTaskEditing(createdTaskItem)
        tasksViewModel.taskEditor.edit { taskDraft -> taskDraft.copy(dueDate = dueDate) }
        tasksViewModel.taskEditor.submit()

        val editedTaskItem = awaitSingleTaskItem { taskItem -> taskItem.task.dueDate == dueDate }
        assertEquals(TaskStatus.DONE, editedTaskItem.task.status)
        assertEquals(createdTaskItem.ratingLevel, editedTaskItem.ratingLevel)
    }

    @Test
    fun movingTaskOutOfDoneClearsRating() = runTest {
        val createdTaskItem = createDoneRatedTask("Ship feature")

        tasksViewModel.moveTask(createdTaskItem, TaskStatus.IN_PROGRESS)

        val movedTaskItem = awaitSingleTaskItem { taskItem -> taskItem.task.status == TaskStatus.IN_PROGRESS }
        assertNull(movedTaskItem.ratingLevel)
    }

    @Test
    fun removingRatingLevelClearsItFromRatedTask() = runTest {
        val createdTaskItem = createDoneRatedTask("Ship feature")
        val usedRatingLevelIndex = awaitContent { true }.ratingLevels.indexOf(createdTaskItem.ratingLevel)

        tasksViewModel.openRatingLevelsEditing()
        tasksViewModel.ratingLevelsEditor.edit { ratingLevelsDraft -> ratingLevelsDraft.removing(usedRatingLevelIndex) }
        tasksViewModel.ratingLevelsEditor.submit()

        val tasksContent = awaitContent { content -> content.taskItems.single().ratingLevel == null }
        assertFalse(createdTaskItem.ratingLevel in tasksContent.ratingLevels)
    }

    @Test
    fun addedReactionMustNotBeBlankBeforeSaving() = runTest {
        tasksViewModel.openRatingLevelsEditing()
        tasksViewModel.addRatingLevel()
        assertFalse(checkNotNull(tasksViewModel.ratingLevelsEditor.dialogState.value).submittable)

        tasksViewModel.ratingLevelsEditor.edit { ratingLevelsDraft ->
            ratingLevelsDraft.replacingReaction(ratingLevelsDraft.ratingLevels.lastIndex, "🔥")
        }
        tasksViewModel.ratingLevelsEditor.submit()

        awaitContent { tasksContent -> tasksContent.ratingLevels.last().reaction == "🔥" }
    }

    @Test
    fun taskDeletionRemovesTask() = runTest {
        createTask("Write plan")
        val createdTaskItem = awaitSingleTaskItem { true }

        tasksViewModel.taskInteractions.onTaskDelete(createdTaskItem)
        tasksViewModel.taskDeletion.submit()

        awaitContent { tasksContent -> tasksContent.taskItems.isEmpty() }
    }

    private suspend fun createDoneRatedTask(title: String): TaskItem {
        createTask(title, TaskStatus.DONE)
        val createdTaskItem = awaitSingleTaskItem { true }
        val bestRatingLevel = awaitContent { true }.ratingLevels.last()

        tasksViewModel.rateTask(createdTaskItem, bestRatingLevel)

        return awaitSingleTaskItem { taskItem -> taskItem.ratingLevel == bestRatingLevel }
    }

    private suspend fun awaitSingleTaskItem(taskItemExpectation: (TaskItem) -> Boolean): TaskItem =
        awaitContent { tasksContent ->
            tasksContent.taskItems
                .singleOrNull()
                ?.let(taskItemExpectation) == true
        }
            .taskItems
            .single()

    private suspend fun awaitContent(contentExpectation: (TasksContent) -> Boolean): TasksContent =
        tasksViewModel.tasksContent.first(contentExpectation)
}
```

#### `shared/ui/tasks/src/jvmTest/kotlin/org/orev/nahidka/ui/tasks/TasksScreenTest.kt` — new

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem
import kotlin.test.Test

private const val TASK_TITLE = "Write plan"

@OptIn(ExperimentalTestApi::class)
class TasksScreenTest : TasksTest() {

    @Test
    fun draggingCardWithMouseMovesTaskToDoneColumn() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)
        val dragDistance = dragDistanceTo("Done (0)")

        onNodeWithText(TASK_TITLE)
            .performMouseInput {
                moveTo(center)
                press()
                moveBy(dragDistance)
                release()
            }

        waitUntil { taskItem()?.task?.status == TaskStatus.DONE }
    }

    @Test
    fun draggingCardAfterLongPressMovesTaskToInProgressColumn() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)
        val dragDistance = dragDistanceTo("In Progress (0)")

        onNodeWithText(TASK_TITLE)
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(dragDistance)
                up()
            }

        waitUntil { taskItem()?.task?.status == TaskStatus.IN_PROGRESS }
    }

    @Test
    fun clickingCardOpensTaskEditor() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 1200.dp)

        onNodeWithText(TASK_TITLE)
            .performClick()

        onNodeWithText("Edit task")
            .assertIsDisplayed()
    }

    @Test
    fun compactCardMenuMovesTaskToDone() = runComposeUiTest {
        createTask(TASK_TITLE)
        showTasksScreen(width = 400.dp)

        onAllNodesWithContentDescription("More actions")
            .onLast()
            .performClick()
        onNodeWithText("Move to Done")
            .performClick()
        onNodeWithText("Done (1)")
            .performClick()

        onNodeWithText(TASK_TITLE)
            .assertIsDisplayed()
        waitUntil { taskItem()?.task?.status == TaskStatus.DONE }
    }

    @Test
    fun listViewRatesDoneTaskWithReaction() = runComposeUiTest {
        createTask(TASK_TITLE, TaskStatus.DONE)
        showTasksScreen(width = 1200.dp)

        onNodeWithText("List")
            .performClick()
        onNodeWithText("Rate")
            .performClick()
        onNodeWithText("🤩")
            .performClick()

        onNodeWithText("🤩")
            .assertIsDisplayed()
        waitUntil { taskItem()?.ratingLevel?.reaction == "🤩" }
    }

    @Test
    fun choosingSelectedReactionAgainRemovesRating() = runComposeUiTest {
        createTask(TASK_TITLE, TaskStatus.DONE)
        showTasksScreen(width = 1200.dp)
        onNodeWithText("Rate")
            .performClick()
        onNodeWithText("😞")
            .performClick()

        onNodeWithText("😞")
            .performClick()
        onAllNodesWithText("😞")
            .onLast()
            .performClick()

        onNodeWithText("Rate")
            .assertIsDisplayed()
        waitUntil { taskItem()?.ratingLevel == null }
    }

    private fun ComposeUiTest.showTasksScreen(width: Dp) {
        setContent {
            TasksScreen(tasksViewModel, Modifier.size(width, 800.dp))
        }
    }

    private fun ComposeUiTest.dragDistanceTo(targetText: String): Offset =
        centerOf(targetText) - centerOf(TASK_TITLE)

    private fun ComposeUiTest.centerOf(text: String): Offset = onNodeWithText(text)
        .fetchSemanticsNode()
        .boundsInRoot
        .center

    private fun taskItem(): TaskItem? = tasksViewModel.tasksContent.value.taskItems
        .firstOrNull { taskItem -> taskItem.task.title == TASK_TITLE }
}
```

### Step 11 — Host integration (`shared/`)

*`:shared` does not compile today for reasons unrelated to this plan (§5), so these edits were type-checked and run through an equivalent scratch test (same `remember { createGraph<TasksScreenGraph>() }` → `viewModel(viewModelStoreOwner = session, key = "tasks") { … }` → `TasksScreen` code), not through `:shared` itself.* Old blocks are against `a63ae84`. The financial plan's Step 9 edits other lines of the same `App.kt`; if you apply it first, only the context lines around `financialManagementViewModel` differ.

#### `shared/src/commonMain/kotlin/org/orev/nahidka/App.kt` — modified

Replaces the hand-made `TasksContext`/`TasksManager` and the deleted `api.TaskStatus` with `TasksScreenGraph` + `TasksScreen`; adds a “Tasks” entry to the temporary top bar (the side menu itself is out of scope).

_Change 1 of 9 (old line 18 / new line 18)_

**Old:**

```kotlin
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.orev.nahidka.api.TaskStatus
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.di.rememberFinancialSession
```

**New:**

```kotlin
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zacsweers.metro.createGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.di.rememberFinancialSession
```

_Change 2 of 9 (old line 37 / new line 37)_

**Old:**

```kotlin
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.common.theme.NahidkaTheme
```

**New:**

```kotlin
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.ui.common.theme.NahidkaTheme
```

_Change 3 of 9 (old line 47 / new line 43)_

**Old:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.settings.SettingsScreen

@Preview
```

**New:**

```kotlin
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.tasks.TasksScreen
import org.orev.nahidka.ui.tasks.TasksScreenGraph
import org.orev.nahidka.ui.tasks.TasksViewModel

@Preview
```

_Change 4 of 9 (old line 78 / new line 77)_

**Old:**

```kotlin

    val session = rememberFinancialSession(sessionConfig)
    val tasksContext = remember(session) { TasksContext() }
    val tasksManager = remember(tasksContext) { TasksManager(tasksContext) }
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
```

**New:**

```kotlin

    val session = rememberFinancialSession(sessionConfig)
    val tasksScreenGraph = remember(session) { createGraph<TasksScreenGraph>() }
    val goalsContext = remember(session) { GoalsContext() }
    val goalsManager = remember(goalsContext) { GoalsManager(goalsContext) }
```

_Change 5 of 9 (old line 85 / new line 83)_

**Old:**

```kotlin
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val tasksSnapshot by tasksContext.tasksState.collectAsState()
    val goalsSnapshot by goalsContext.goalsState.collectAsState()
    val batterySnapshot by batteryContext.batteryState.collectAsState()
```

**New:**

```kotlin
    val batteryManager = remember(batteryContext) { SocialBatteryManager(batteryContext) }
    val identifierGenerator = remember { RandomIdentifierGenerator() }
    val goalsSnapshot by goalsContext.goalsState.collectAsState()
    val batterySnapshot by batteryContext.batteryState.collectAsState()
```

_Change 6 of 9 (old line 105 / new line 102)_

**Old:**

```kotlin
        mutableStateOf(false)
    }

    val dashboardViewModel = viewModel<DashboardViewModel>(
```

**New:**

```kotlin
        mutableStateOf(false)
    }
    var showingTasks by remember { mutableStateOf(false) }

    val dashboardViewModel = viewModel<DashboardViewModel>(
```

_Change 7 of 9 (old line 111 / new line 109)_

**Old:**

```kotlin
    ) {
        session.graph.dashboardViewModel
    }
```

**New:**

```kotlin
    ) {
        session.graph.dashboardViewModel
    }

    val tasksViewModel = viewModel<TasksViewModel>(
        viewModelStoreOwner = session,
        key = "tasks",
    ) {
        tasksScreenGraph.tasksViewModel
    }
```

_Change 8 of 9 (old line 125 / new line 130)_

**Old:**

```kotlin
            titleBar?.invoke()
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { showingSettings = false; showingFinance = false; selectedPersonalFeature = null }) { Text("Overview") }
                TextButton(modifier = Modifier.testTag("open-settings"), onClick = { showingSettings = true; selectedPersonalFeature = null }) { Text("Settings") }
                PersonalFeature.entries.forEach { feature ->
                    TextButton(onClick = { showingSettings = false; selectedPersonalFeature = feature }) {
                        Text(feature.name.lowercase().replace('_', ' '))
                    }
```

**New:**

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

_Change 9 of 9 (old line 147 / new line 153)_

**Old:**

```kotlin
                        }
                    }, errorMessage = settingsError)
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        tasks = tasksSnapshot.tasks,
                        goals = goalsSnapshot.goals,
                        batteryPercentage = batterySnapshot.socialBattery?.percentage,
                        onCreateTask = { title -> mutatePersonalFeature { tasksManager.createTasks(listOf(TaskCreationRequest(identifierGenerator.next(), title))) } },
                        onUpdateTask = { task -> mutatePersonalFeature {
                            val nextStatus = TaskStatus.entries[(task.status.ordinal + 1) % TaskStatus.entries.size]
                            tasksManager.updateTasks(listOf(TaskUpdateRequest(task.identifier, status = nextStatus)))
                        } },
                        onCreateGoal = { title -> mutatePersonalFeature { goalsManager.createGoal(GoalCreationRequest(identifierGenerator.next(), title)) } },
                        onUpdateGoal = { goal -> mutatePersonalFeature {
```

**New:**

```kotlin
                        }
                    }, errorMessage = settingsError)
                } else if (showingTasks) {
                    TasksScreen(tasksViewModel)
                } else if (selectedPersonalFeature != null) {
                    PersonalFeaturesScreen(
                        feature = requireNotNull(selectedPersonalFeature),
                        goals = goalsSnapshot.goals,
                        batteryPercentage = batterySnapshot.socialBattery?.percentage,
                        onCreateGoal = { title -> mutatePersonalFeature { goalsManager.createGoal(GoalCreationRequest(identifierGenerator.next(), title)) } },
                        onUpdateGoal = { goal -> mutatePersonalFeature {
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/PersonalFeature.kt` — modified

Tasks are no longer a “personal feature” tab.

**Old:**

```kotlin
package org.orev.nahidka

internal enum class PersonalFeature { TASKS, GOALS, SOCIAL_BATTERY }
```

**New:**

```kotlin
package org.orev.nahidka

internal enum class PersonalFeature { GOALS, SOCIAL_BATTERY }
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/PersonalFeaturesScreen.kt` — modified

Drops the old tasks table (its `ui.task.TasksTable` was deleted in `409e1fe`).

_Change 1 of 3 (old line 7 / new line 7)_

**Old:**

```kotlin
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.*
import org.orev.nahidka.feature.tasks.dto.*
import org.orev.nahidka.ui.goal.GoalsTable
import org.orev.nahidka.ui.socialbattery.SocialBatteryWidget
import org.orev.nahidka.ui.task.TasksTable

@Composable
internal fun PersonalFeaturesScreen(
    feature: PersonalFeature,
    tasks: List<TaskRecord>,
    goals: List<GoalRecord>,
    batteryPercentage: Int?,
    onCreateTask: (String) -> Unit,
    onUpdateTask: (TaskRecord) -> Unit,
    onCreateGoal: (String) -> Unit,
    onUpdateGoal: (GoalRecord) -> Unit,
```

**New:**

```kotlin
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
```

_Change 2 of 3 (old line 28 / new line 23)_

**Old:**

```kotlin
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (feature != PersonalFeature.SOCIAL_BATTERY) {
            Text(if (feature == PersonalFeature.TASKS) "Tasks" else "Goals", style = MaterialTheme.typography.headlineMedium)
            Row {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f))
                TextButton(enabled = newTitle.isNotBlank(), onClick = {
                    if (feature == PersonalFeature.TASKS) onCreateTask(newTitle.trim()) else onCreateGoal(newTitle.trim())
                    newTitle = ""
                }) { Text("Add") }
```

**New:**

```kotlin
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (feature != PersonalFeature.SOCIAL_BATTERY) {
            Text("Goals", style = MaterialTheme.typography.headlineMedium)
            Row {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f))
                TextButton(enabled = newTitle.isNotBlank(), onClick = {
                    onCreateGoal(newTitle.trim())
                    newTitle = ""
                }) { Text("Add") }
```

_Change 3 of 3 (old line 39 / new line 34)_

**Old:**

```kotlin
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
            PersonalFeature.TASKS -> {
                Text("Select a task to advance its status.")
                TasksTable(tasks, onTaskClick = onUpdateTask)
            }
            PersonalFeature.GOALS -> {
                Text("Select a goal to advance its progress by 10%.")
```

**New:**

```kotlin
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
            PersonalFeature.GOALS -> {
                Text("Select a goal to advance its progress by 10%.")
```

---

## 5. Verification, limitations, out of scope

**Verified (in an isolated copy of the repository):**
* `:shared:core:feature:tasks`, `:shared:ui:common` and `:shared:ui:tasks` compile for **JVM, JS, Wasm and Android**, main and test sources. The new code produces no compiler warnings. The only warnings are pre-existing ones in `core:common` and `shared/build.gradle.kts`.
* **21 / 21 tests pass, green on 3 consecutive forced re-runs:** 5 core (`TasksContextTest`), 3 `DialogControllerTest`, 7 `TasksViewModelTest`, and 6 `TasksScreenTest`. The UI tests drive real input: a mouse drag, a touch long-press drag, the compact ⋮ menu, rating with a reaction, removing it by tapping the same reaction again, and click-to-edit. With the drop handler disabled, both drag tests fail; with the reaction toggle disabled, the removal test fails.
* Rendered headlessly from this code: the desktop board in light and dark themes, a drag in progress, the list view, the editor with the reaction bar, and the rating-reactions dialog, plus the phone board, list and editor, including Ukrainian at 360 dp. The headless renderer on the Linux build machine draws 😞 and 😐 as outline glyphs (font fallback); devices draw color emoji.
* Both `README.md` examples (the existing one and the new ratings one) were run as code.
* On Android, Compose resources are packaged by `copyAndroidMainComposeResourcesToAndroidAssets`; no extra build flag is needed.

**Not verified:**
* **iOS**: it can't be built on this Linux host.
* **`:shared` (Step 11)**: this module doesn't compile today, independently of this plan. `App.kt`, `FinancialSessionGraph.kt` and `DashboardViewModel` still reference code deleted in `409e1fe` (`NahidkaTheme`, `StateHolder`, `SettingsScreen`, `GoalsTable`, `SocialBatteryWidget`, `FinancialManagementViewModel`). Also, `:shared:core:feature:goals` has the same broken typealias that Step 1 fixes for tasks (`GoalRecord = api.Goal`). I ran the host wiring through an equivalent scratch test instead.

**Known limitations:**
* Tasks stay in memory, like the core today: persistence and back end are not part of this plan.
* On touch, a long press released without moving also opens the editor (the click fires after the press).
* Drag & drop doesn't auto-scroll a long column, and it doesn't choose a position inside the column (decision §2.3).

**Out of scope:** the side menu and app shell; repairing `core/feature/goals`, which takes the same fix as Step 1; overdue highlighting; sorting and filtering in the list view.

**Follow-up for `shared/ui/financial-management/IMPLEMENTATION_PLAN.md`** (not implemented yet). Its generic pieces map one-to-one onto `shared/ui/common`, so that plan shrinks:

| Financial plan | `shared/ui/common` |
|---|---|
| `FinancialLayoutWidth` | `LayoutWidth` |
| `FinancialChoiceSelector` | `ChoiceSelector` |
| `FinancialAddButton` | `AddButton` |
| `FinancialDialogState` / `FinancialDialogController` / `FinancialDialog` | `DialogState` / `DialogController<Draft, FinancialError>` / `ControlledDialog(rejectionText = financialErrorText)` |
| `FinancialDateField` | `DateField` (without `onDateClear`) |
| `FINANCIAL_DAY_FORMAT` | `DAY_FORMAT` |
| `FinancialTable` | `DataTable` + a `footer` slot for its paging footer |
| `FinancialEntryActionsMenu` | `MoreActionsMenu` (built on `DropdownPopup`) |

I can revise that plan once you approve this one.

**Suggested implementation order:** Steps 1 → 11, in order. The core compiles and its tests pass after Step 1, `ui:common` after Step 2, and `ui:tasks` once Steps 3–9 are in; then Step 10 (tests). Do Step 11 once `:shared` builds again.
