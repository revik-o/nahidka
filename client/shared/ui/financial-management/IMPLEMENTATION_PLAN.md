# Implementation plan — `shared/ui/financial-management`

**Status:** Steps 1–9 are implemented in this worktree. Current verification, centralized mock data, and design limitations are recorded in [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). The code blocks and original verification notes below preserve the implementation proposal.

**Revision 2:** categories are no longer created inside these dialogs. They are selected from existing ones, and creation moves to a separate categories page (see §2.2).

---

## 1. What gets built (mockup → implementation)

| Mockup element | Implementation |
|---|---|
| Tabs "Finance history / Finance planning" | Segmented selector at the top-left (`FinancialChoiceSelector`), state kept with `rememberSaveable` |
| Selector for currency | Segmented selector over `FinancialSessionConfig.assets`; the selection is shared with planning (`FinancialAssetSelection`) |
| Add financial operation | Dialog: Expense/Income · amount · category (picked from existing categories) · Card/Crypto/Cash · date |
| History table, no column titles | Rows: category icon · category · `↓`/`↑` amount · payment type · date · ✎ 🗑 |
| Empty transactions message | "There are no transactions yet" inside the table frame |
| Scroll to bottom → load or "that's all data" | 50 rows per page; when the footer is reached the next page loads; when nothing is left the footer shows "That is all data" |
| `‹ selected month ›` | Month selector (`10.2026`), previous/next month |
| Add category for planning | Dialog: category (picked from existing categories not yet planned this month) · planned amount · payment type. The first row creates the month's planning table |
| Amount of money / How much is left / Can save money ~ | Three summary lines computed by the core (mapping in §2.1) |
| Planning table, no column titles | Rows: category icon · category · planned sum · payment type · ✎ 🗑; empty message |
| Edit / delete on each row | ✎ opens the same dialog pre-filled; 🗑 opens a confirmation dialog |
| SIDE MENU | The app shell, outside this module (unchanged) |

### Mobile adaptivity

The screen measures **its own width** (`BoxWithConstraints`) rather than the window size, so a narrow desktop window behaves like a phone. The breakpoint is 600 dp (the Material "compact" class).

| | Expanded (≥ 600 dp) | Compact (< 600 dp) |
|---|---|---|
| Header | One row: tabs on the left, actions on the right | Tabs at full width; actions on a second, right-aligned row |
| Add button | Text button ("Add financial operation") | Round `＋` icon button (label kept as its content description) |
| Table row | One line of weighted columns: icon · name · amount · payment · date · ✎ · 🗑 | Two-line item: icon · (name / payment) · (amount / date) · `⋮` menu with Edit/Delete |
| Dialogs | Material `AlertDialog` | Same; content scrolls vertically |
| Padding | 24 dp | 16 dp |

---

## 2. Decisions I made — please confirm or redirect before I implement

1. **Summary numbers.** The core already computes these values; the UI only maps them:

   | Mockup line | With a plan (`PlanningConfigured`) | Without a plan yet (`PlanningNotConfigured`) |
   |---|---|---|
   | Amount of money | `totals.fundsAvailableThisMonth` (opening balance + this month's income) | `income` |
   | How much is left | `totals.currentAvailable` (amount − net spent) | `currentAvailable` (**new core field**, same formula) |
   | Can save money ~ | `totals.projectedAvailableAfterPlanning` (left − what planned categories still reserve) | `currentAvailable` |

   The opening balance is always `0`, and `savingsPolicy` stays `null`, because the mockup has no input for either. *If you want "Amount of money" to be editable (for example, money carried over from last month), I can add one more small dialog built from the same components.*
2. **Categories are selected here and created on a separate page** (your decision). Both dialogs use one read-only dropdown (`FinancialCategoryField`) listing the active categories; planning also hides categories already planned this month. Save stays disabled until a category is picked. When no categories exist, the field shows "There are no categories yet. Create one on the categories page". **The categories page itself is not part of this plan**, and until it exists the app has no way to create a category, so operations and planning rows can't be added in the running app. Rows show `FinancialCategory.iconName` as an emoji (or the first letter of the name): the version catalog has no Material icons artifact, and the dashboard already uses glyphs.
3. **"Load more" is UI paging over the live subscription.** The core has no paging API, and the in-memory gateway already materializes the full snapshot, so the ViewModel exposes `take(50 × pages)` plus `hasMoreRows`. When a remote backend arrives, a core paging query replaces `displayedRowLimit`, and the ViewModel/UI contract stays the same.
4. **One currency selection for both tabs.** `FinancialAssetSelection` is `@SingleIn(FinancialSessionScope)`. Per the mockup, the selector appears only on the history tab, and planning uses the same currency.
5. **Operation kinds.** The editor creates an *Expense* or an *Income*. Refunds are shown (`↑`) and can be edited or deleted, but not created, because creating one needs a parent-expense picker that the mockup doesn't have. The kind is fixed when editing, which is a core rule.
6. **History covers all months**, newest first, because the mockup has no month filter there. **Delete asks for confirmation.**
7. **Two ViewModels (SRP)** replace the deleted 420-line `FinancialManagementViewModel`. `ui/common` (`StateHolder`) was deleted in `409e1fe`, so the ViewModels extend `androidx.lifecycle.ViewModel` directly. They expose `StateFlow`s plus intent methods, and every dialog is a generic `FinancialDialogController<Draft>`.
8. **Strings** are Compose resources with **en / uk / ru** translations.

**How your style rules are applied:** there are no comments anywhere (tests included). Names are full words with no abbreviations, and there is **no implicit `it`**: every lambda parameter is named. Builder, subscription, flow and `Modifier` chains are written one call per line. `@Inject` is class-level, which is what Metro recommends: the compiler warns on constructor-level. Imports follow the IntelliJ layout from your last commit (wildcards from 5 names, `kotlin.*` last).

---

## 3. Architecture

```
ui/financialmanagement
├── FinancialManagementScreen        tab state + width class; hosts both tabs and their dialogs
├── FinancialManagementTab           HISTORY | PLANNING (+ title resource)
├── FinancialAssetSelection          session-scoped selected currency, shared by both ViewModels
├── common/                          non-visual building blocks
│   ├── FinancialContentState        Loading | Unavailable | Available(content) + Flow.stateInFinancialContent()
│   ├── FinancialDialogController    open / edit / submit / dismiss; maps MutationResult → dialog state
│   ├── FinancialDialogState         draft · submittable · submitting · rejection
│   ├── FinancialEntityObservation   FinancialSubscription<T> → Flow<PersistentMap<id, T>> (snapshot + batches)
│   ├── FinancialCommandMeta         IdentifierGenerator.nextCommandMeta()
│   └── FinancialMoneyParsing        parsePositiveMoney()
├── component/                       composables reused by both tabs
│   ├── FinancialLayoutWidth         COMPACT | EXPANDED (600 dp)
│   ├── FinancialTabLayout           adaptive header (tabs + actions) + content
│   ├── FinancialChoiceSelector      generic segmented selector (tabs, currency, kind, payment)
│   ├── FinancialAddButton           text button ↔ ＋ icon button
│   ├── FinancialContent             loading / unavailable / content switch
│   ├── FinancialTable               bordered lazy list, empty message, optional footer
│   ├── FinancialTableEntry          row contract implemented by history and planning rows
│   ├── FinancialTableRow            adaptive row (columns ↔ two-line + ⋮ menu)
│   ├── FinancialDialog              generic AlertDialog bound to a FinancialDialogController
│   ├── FinancialCategoryField       read-only dropdown of existing categories (+ empty-list hint)
│   ├── FinancialMoneyField / FinancialDateField / FinancialCategoryIcon
│   └── FinancialErrorText / FinancialPaymentMethodTitle / FinancialDateFormats / FinancialAmountDirection
├── history/                         FinancialHistoryViewModel, content/row/draft models, tab, dialogs
└── planning/                        FinancialPlanningViewModel, content/row/draft/summary models, tab, dialogs, month selector
```

**Data flow**

* **History:** `selectedAsset` → `flatMapLatest` → `combine(subscribeOperations(asset), subscribeCategories(), displayedRowLimit)` → `FinancialHistoryContent` → `StateFlow<FinancialContentState<…>>`
* **Planning:** `combine(selectedMonth, selectedAsset)` → `MonthlyQuery` → `flatMapLatest(observeFinancialSnapshot)` → `FinancialPlanningContent`
* **Mutations:** Save → `FinancialDialogController.submit()` → gateway command → `Committed`: dialog closes / `Rejected`: the error shows inside the dialog. Lists update through the subscriptions; there is no manual refresh.

**DRY map**

| Shared piece | Used by |
|---|---|
| `FinancialDialogController` + `FinancialDialog` | operation editor, operation deletion, planning-row editor, planning-row deletion |
| `FinancialCategoryField` + `FinancialCategoryIcon` | operation editor, planning-row editor, table rows |
| `FinancialTable` + `FinancialTableRow` + `FinancialTableEntry` | history table, planning table (both layouts) |
| `FinancialChoiceSelector` | tabs, currency, operation kind, payment method ×2 |
| `FinancialContentState` + `FinancialContent` | history, planning |
| `FinancialTabLayout` + `FinancialAddButton` | history header, planning header |
| `FinancialMoneyField` + `parsePositiveMoney` | amount, planned amount, both drafts' validation |
| Core `FinancialOperationRecencyComparator` / `FinancialCategoryNameComparator` | core snapshots *and* UI re-sorting after deltas |

---

## 4. Implementation steps

Each file shows **Old** (empty for a new file) and **New**. Modified files show only the changed regions, with two lines of context.

### Step 1 — Core additions (`shared/core/feature/financial-management`)

Small, DRY-driven changes: the UI needs the same orderings the core uses, the current month from a clock, and "how much is left" when no plan exists.

#### `shared/core/feature/financial-management/src/commonMain/kotlin/org/orev/nahidka/feature/financial/calculation/FinancialOperationRecencyComparator.kt` — new

New public comparator (newest first, then identifier); replaces two inline copies in core and is reused by the history ViewModel.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.FinancialOperation

object FinancialOperationRecencyComparator : Comparator<FinancialOperation> {

    override fun compare(first: FinancialOperation, second: FinancialOperation): Int {
        val occurrenceOrder = second.occurredAt.compareTo(first.occurredAt)

        return if (occurrenceOrder != 0) {
            occurrenceOrder
        } else {
            first.identifier.compareTo(second.identifier)
        }
    }
}
```

#### `shared/core/feature/financial-management/src/commonMain/kotlin/org/orev/nahidka/feature/financial/subscription/OperationWatchSource.kt` — modified

Use the shared comparator.

_Change 1 of 2 (old line 5 / new line 5)_

**Old:**

```kotlin
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.core.common.ErrorReporter
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.FinancialCommit
```

**New:**

```kotlin
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.core.common.ErrorReporter
import org.orev.nahidka.feature.financial.calculation.FinancialOperationRecencyComparator
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.FinancialCommit
```

_Change 2 of 2 (old line 20 / new line 21)_

**Old:**

```kotlin
        val entities = frame.data.operations.values
            .filter(::matches)
            .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.identifier })
            .toPersistentList()

```

**New:**

```kotlin
        val entities = frame.data.operations.values
            .filter(::matches)
            .sortedWith(FinancialOperationRecencyComparator)
            .toPersistentList()

```

#### `shared/core/feature/financial-management/src/commonMain/kotlin/org/orev/nahidka/feature/financial/calculation/FinancialCategoryNameComparator.kt` — modified

Make public so the UI sorts categories exactly like core snapshots.

**Old:**

```kotlin
import org.orev.nahidka.feature.financial.dto.FinancialCategory

internal object FinancialCategoryNameComparator : Comparator<FinancialCategory> {

    override fun compare(first: FinancialCategory, second: FinancialCategory): Int {
```

**New:**

```kotlin
import org.orev.nahidka.feature.financial.dto.FinancialCategory

object FinancialCategoryNameComparator : Comparator<FinancialCategory> {

    override fun compare(first: FinancialCategory, second: FinancialCategory): Int {
```

#### `shared/core/feature/financial-management/src/commonMain/kotlin/org/orev/nahidka/feature/financial/dto/PlanningNotConfigured.kt` — modified

Add `currentAvailable`, so "How much is left" is computed by core even without a plan.

**Old:**

```kotlin
    val refunds: Money,
    val netExpense: Money,
) : PlanningState
```

**New:**

```kotlin
    val refunds: Money,
    val netExpense: Money,
    val currentAvailable: Money,
) : PlanningState
```

#### `shared/core/feature/financial-management/src/commonMain/kotlin/org/orev/nahidka/feature/financial/calculation/FinancialSummaryCalculator.kt` — modified

Use the shared comparator; compute `currentAvailable`; add `financialMonthFor(instant, …)` (the planning ViewModel uses it for the current month).

_Change 1 of 5 (old line 6 / new line 6)_

**Old:**

```kotlin
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.InternalFrame

private const val UNCATEGORIZED_IDENTIFIER = "projection:uncategorized"
```

**New:**

```kotlin
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.InternalFrame
import kotlin.time.Instant

private const val UNCATEGORIZED_IDENTIFIER = "projection:uncategorized"
```

_Change 2 of 5 (old line 265 / new line 267)_

**Old:**

```kotlin
        .asSequence()
        .filter { it.amount.assetIdentifier == query.assetIdentifier && it.occurredAt >= period.startInclusive && it.occurredAt < period.endExclusive }
        .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.identifier })
        .toPersistentList()

```

**New:**

```kotlin
        .asSequence()
        .filter { it.amount.assetIdentifier == query.assetIdentifier && it.occurredAt >= period.startInclusive && it.occurredAt < period.endExclusive }
        .sortedWith(FinancialOperationRecencyComparator)
        .toPersistentList()

```

_Change 3 of 5 (old line 287 / new line 289)_

**Old:**

```kotlin
        }

        PlanningNotConfigured(
            period = period,
```

**New:**

```kotlin
        }

        val netExpense = checkedSubtract(grossExpenses, refunds)

        PlanningNotConfigured(
            period = period,
```

_Change 4 of 5 (old line 292 / new line 296)_

**Old:**

```kotlin
            grossExpenses = Money(query.assetIdentifier, grossExpenses),
            refunds = Money(query.assetIdentifier, refunds),
            netExpense = Money(query.assetIdentifier, checkedSubtract(grossExpenses, refunds)),
        )
    } else {
```

**New:**

```kotlin
            grossExpenses = Money(query.assetIdentifier, grossExpenses),
            refunds = Money(query.assetIdentifier, refunds),
            netExpense = Money(query.assetIdentifier, netExpense),
            currentAvailable = Money(query.assetIdentifier, checkedSubtract(income, netExpense)),
        )
    } else {
```

_Change 5 of 5 (old line 310 / new line 315)_

**Old:**

```kotlin
}

fun financialMonthFor(operation: FinancialOperation, reportingTimeZone: String): YearMonth =
    operation.occurredAt.toLocalDateTime(TimeZone.of(reportingTimeZone)).date.let { date ->
        YearMonth(date.year, date.month.ordinal + 1)
    }
```

**New:**

```kotlin
}

fun financialMonthFor(instant: Instant, reportingTimeZone: String): YearMonth =
    instant.toLocalDateTime(TimeZone.of(reportingTimeZone)).date.yearMonth

fun financialMonthFor(operation: FinancialOperation, reportingTimeZone: String): YearMonth =
    financialMonthFor(operation.occurredAt, reportingTimeZone)
```

#### `shared/core/feature/financial-management/README.md` — modified

Keep the module README in sync (new field and overload; links to the new ViewModels instead of the deleted one).

_Change 1 of 3 (old line 252 / new line 252)_

**Old:**

~~~markdown
// Snapshot order: operations = occurredAt descending, then identifier;
// categories = name ignoring case, then identifier; planning = month, asset, identifier.
// Re-sort UI lists yourself after applying deltas. Change.index is a commit delta
// index, NOT a position in the query result.
EntitySnapshot<T>(sessionIdentity: String, storeRevision: Long, entities: PersistentList<T>)
~~~

**New:**

~~~markdown
// Snapshot order: operations = occurredAt descending, then identifier;
// categories = name ignoring case, then identifier; planning = month, asset, identifier.
// Re-sort UI lists yourself after applying deltas (FinancialOperationRecencyComparator,
// FinancialCategoryNameComparator reproduce the snapshot orders). Change.index is a commit delta
// index, NOT a position in the query result.
EntitySnapshot<T>(sessionIdentity: String, storeRevision: Long, entities: PersistentList<T>)
~~~

_Change 2 of 3 (old line 322 / new line 323)_

**Old:**

~~~markdown

PlanningState = PlanningConfigured(table: PlanningTableView) | PlanningNotConfigured
PlanningNotConfigured = period, income, grossExpenses, refunds, netExpense
SpendingSummary = storeRevision, period, assetIdentifier, grossExpense, refunds, netExpense,
                  drawableTotal, refundCredits, slices: PersistentList<SpendingSlice>
~~~

**New:**

~~~markdown

PlanningState = PlanningConfigured(table: PlanningTableView) | PlanningNotConfigured
PlanningNotConfigured = period, income, grossExpenses, refunds, netExpense, currentAvailable
SpendingSummary = storeRevision, period, assetIdentifier, grossExpense, refunds, netExpense,
                  drawableTotal, refundCredits, slices: PersistentList<SpendingSlice>
~~~

_Change 3 of 3 (old line 363 / new line 364)_

**Old:**

~~~markdown
// Calculators filter by asset/period, use checked arithmetic, and do not validate
// imported documents as gateway mutations do. Supply valid domain records.
// financialMonthFor(operation, reportingTimeZone): YearMonth
// calendar.reportingPeriod(month, timeZoneIdentifier): ReportingPeriod
// ReportingPeriod: month, timeZoneIdentifier, startInclusive: Instant, endExclusive: Instant.
```

Source: [gateway contract](src/commonMain/kotlin/org/orev/nahidka/feature/financial/gateway/FinancialGateway.kt), [mutation rules](src/commonMain/kotlin/org/orev/nahidka/feature/financial/store/FinancialReducer.kt), [calculations](src/commonMain/kotlin/org/orev/nahidka/feature/financial/calculation/FinancialSummaryCalculator.kt). Host integration: [FinancialSessionGraph](../../../src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionGraph.kt), [session owner](../../../src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionOwner.kt). Usage/tests: [FinancialManagementViewModel](../../../ui/financialmanagement/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialManagementViewModel.kt), [view-model tests](../../../ui/financialmanagement/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialManagementViewModelTest.kt).
~~~

**New:**

~~~markdown
// Calculators filter by asset/period, use checked arithmetic, and do not validate
// imported documents as gateway mutations do. Supply valid domain records.
// financialMonthFor(operation, reportingTimeZone) / financialMonthFor(instant, reportingTimeZone): YearMonth
// calendar.reportingPeriod(month, timeZoneIdentifier): ReportingPeriod
// ReportingPeriod: month, timeZoneIdentifier, startInclusive: Instant, endExclusive: Instant.
```

Source: [gateway contract](src/commonMain/kotlin/org/orev/nahidka/feature/financial/gateway/FinancialGateway.kt), [mutation rules](src/commonMain/kotlin/org/orev/nahidka/feature/financial/store/FinancialReducer.kt), [calculations](src/commonMain/kotlin/org/orev/nahidka/feature/financial/calculation/FinancialSummaryCalculator.kt). Host integration: [FinancialSessionGraph](../../../src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionGraph.kt), [session owner](../../../src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionOwner.kt). Usage/tests: [FinancialHistoryViewModel](../../../ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryViewModel.kt), [FinancialPlanningViewModel](../../../ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningViewModel.kt), [view-model tests](../../../ui/financial-management/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement).
~~~

### Step 2 — UI building blocks (`common/`)

Non-visual pieces shared by both tabs.

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/common/FinancialContentState.kt` — new

Loading / Unavailable / Available state and the single `stateIn` policy for both ViewModels.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

private const val FINANCIAL_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS = 5_000L

sealed interface FinancialContentState<out Content> {

    data object Loading : FinancialContentState<Nothing>

    data object Unavailable : FinancialContentState<Nothing>

    data class Available<Content>(val content: Content) : FinancialContentState<Content>
}

internal fun <Content> FinancialContentState<Content>.availableContentOrNull(): Content? =
    (this as? FinancialContentState.Available)?.content

internal fun <Content> Flow<Content>.stateInFinancialContent(
    coroutineScope: CoroutineScope,
): StateFlow<FinancialContentState<Content>> =
    this
        .map<Content, FinancialContentState<Content>> { content -> FinancialContentState.Available(content) }
        .catch { emit(FinancialContentState.Unavailable) }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(FINANCIAL_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS),
            initialValue = FinancialContentState.Loading,
        )
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/common/FinancialDialogState.kt` — new

Immutable dialog state.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.common

import org.orev.nahidka.feature.financial.dto.FinancialError

data class FinancialDialogState<Draft>(
    val draft: Draft,
    val submittable: Boolean,
    val submitting: Boolean = false,
    val rejection: FinancialError? = null,
)
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/common/FinancialDialogController.kt` — new

One generic dialog lifecycle for all four dialogs: validation gate, double-submit guard, rejection display.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.financial.dto.MutationResult

class FinancialDialogController<Draft> internal constructor(
    private val coroutineScope: CoroutineScope,
    private val draftValidation: (Draft) -> Boolean = { true },
    private val mutation: suspend (Draft) -> MutationResult<*>,
) {

    private val mutableDialogState = MutableStateFlow<FinancialDialogState<Draft>?>(null)

    val dialogState: StateFlow<FinancialDialogState<Draft>?> = mutableDialogState.asStateFlow()

    fun open(draft: Draft) {
        mutableDialogState.value = FinancialDialogState(draft, draftValidation(draft))
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
            when (val mutationResult = mutation(submittedDialogState.draft)) {
                is MutationResult.Committed -> dismiss()
                is MutationResult.Rejected -> mutableDialogState.update { openedDialogState ->
                    openedDialogState?.copy(submitting = false, rejection = mutationResult.error)
                }
            }
        }
    }

    fun dismiss() {
        mutableDialogState.value = null
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/common/FinancialEntityObservation.kt` — new

Turns the core builder-style subscription into a cold `Flow` keyed by identifier (snapshot/resync replace, batches apply Insert/Update/Delete).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.common

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.orev.nahidka.feature.financial.dto.EntityChange
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription

internal fun <Entity> FinancialSubscription<Entity>.observeEntities(
    entityIdentifier: (Entity) -> String,
): Flow<PersistentMap<String, Entity>> = callbackFlow {
    var entitiesByIdentifier = persistentMapOf<String, Entity>()

    this@observeEntities
        .onSnapshot { entitySnapshot ->
            entitiesByIdentifier = entitySnapshot.entities
                .associateBy(entityIdentifier)
                .toPersistentMap()
            send(entitiesByIdentifier)
        }
        .onBatch { changeBatch ->
            entitiesByIdentifier = changeBatch.changes.fold(entitiesByIdentifier) { changedEntities, entityChange ->
                when (entityChange) {
                    is EntityChange.Insert -> changedEntities.putting(entityIdentifier(entityChange.after), entityChange.after)
                    is EntityChange.Update -> changedEntities.putting(entityIdentifier(entityChange.after), entityChange.after)
                    is EntityChange.Delete -> changedEntities.removing(entityIdentifier(entityChange.before))
                }
            }
            send(entitiesByIdentifier)
        }
        .onError { failure -> close(failure) }
        .launchIn(this)

    awaitClose()
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/common/FinancialCommandMeta.kt` — new

`CommandMeta(identifierGenerator.next())` in one place.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.common

import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.feature.financial.command.CommandMeta

internal fun IdentifierGenerator.nextCommandMeta(): CommandMeta = CommandMeta(next())
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/common/FinancialMoneyParsing.kt` — new

Exact positive money parsing on top of core `parseMoneyText`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.common

import org.orev.nahidka.feature.financial.calculation.parseMoneyText
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.Money

internal fun parsePositiveMoney(amountText: String, asset: AssetDefinition): Money? =
    parseMoneyText(amountText, asset)?.takeIf { money -> money.units > 0 }
```

### Step 3 — Shared composables (`component/`)

Everything visual that both tabs reuse, including all of the mobile adaptivity.

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialLayoutWidth.kt` — new

The single breakpoint (600 dp).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FINANCIAL_COMPACT_LAYOUT_WIDTH_LIMIT = 600.dp

internal enum class FinancialLayoutWidth {
    COMPACT,
    EXPANDED;

    companion object {

        fun of(availableWidth: Dp): FinancialLayoutWidth =
            if (availableWidth < FINANCIAL_COMPACT_LAYOUT_WIDTH_LIMIT) COMPACT else EXPANDED
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialTabLayout.kt` — new

Adaptive header: one row when expanded, two rows when compact.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun FinancialTabLayout(
    financialLayoutWidth: FinancialLayoutWidth,
    tabSelector: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (financialLayoutWidth == FinancialLayoutWidth.COMPACT) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (financialLayoutWidth) {
            FinancialLayoutWidth.COMPACT -> {
                tabSelector()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }

            FinancialLayoutWidth.EXPANDED -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabSelector()
                Spacer(Modifier.weight(1f))
                actions()
            }
        }
        content()
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialChoiceSelector.kt` — new

Generic segmented selector. `width(IntrinsicSize.Max)` on the label works around Material's `IntrinsicSize.Min` row, which otherwise ellipsizes labels on desktop.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

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
internal fun <Option> FinancialChoiceSelector(
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

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialAddButton.kt` — new

Text button when expanded, `＋` icon button when compact.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun FinancialAddButton(
    title: String,
    financialLayoutWidth: FinancialLayoutWidth,
    onClick: () -> Unit,
) {
    when (financialLayoutWidth) {
        FinancialLayoutWidth.COMPACT -> FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = title },
        ) {
            Text("＋")
        }

        FinancialLayoutWidth.EXPANDED -> Button(onClick = onClick) {
            Text(title)
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialContent.kt` — new

Renders `FinancialContentState`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_content_unavailable
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.common.FinancialContentState

@Composable
internal fun <Content> FinancialContent(
    financialContentState: FinancialContentState<Content>,
    content: @Composable (Content) -> Unit,
) {
    when (financialContentState) {
        FinancialContentState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        FinancialContentState.Unavailable -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.financialmanagement_content_unavailable))
        }

        is FinancialContentState.Available -> content(financialContentState.content)
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialTable.kt` — new

Bordered lazy table without column titles, with an empty message and an optional footer.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

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

private const val FINANCIAL_TABLE_FOOTER_KEY = "financial-table-footer"

@Composable
internal fun <Row> FinancialTable(
    rows: List<Row>,
    rowKey: (Row) -> Any,
    emptyTableMessage: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
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
                items(rows, key = rowKey) { row ->
                    rowContent(row)
                    HorizontalDivider()
                }
                footer?.let { tableFooter ->
                    item(key = FINANCIAL_TABLE_FOOTER_KEY) { tableFooter() }
                }
            }
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialTableEntry.kt` — new

Row contract implemented by both row models.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.PaymentMethod

interface FinancialTableEntry {
    val categoryIconName: String?
    val categoryName: String
    val formattedAmount: String
    val amountDirection: FinancialAmountDirection?
    val paymentMethod: PaymentMethod?
    val occurredOn: LocalDate?
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialAmountDirection.kt` — new

Incoming (↑) / outgoing (↓).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

enum class FinancialAmountDirection {
    INCOMING,
    OUTGOING,
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialTableRow.kt` — new

Adaptive row: weighted columns with ✎ 🗑 when expanded; two-line item with a ⋮ menu when compact.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_delete
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_edit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FinancialTableRow(
    financialTableEntry: FinancialTableEntry,
    financialLayoutWidth: FinancialLayoutWidth,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(if (financialLayoutWidth == FinancialLayoutWidth.COMPACT) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinancialCategoryIcon(financialTableEntry.categoryIconName, financialTableEntry.categoryName)
        when (financialLayoutWidth) {
            FinancialLayoutWidth.COMPACT -> {
                Column(Modifier.weight(1f)) {
                    Text(financialTableEntry.categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    financialTableEntry.paymentMethod?.let { paymentMethod ->
                        FinancialEntrySupportingText(stringResource(paymentMethod.title))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    FinancialEntryAmount(financialTableEntry)
                    financialTableEntry.occurredOn?.let { occurredOn ->
                        FinancialEntrySupportingText(occurredOn.format(FINANCIAL_DAY_FORMAT))
                    }
                }
                FinancialEntryActionsMenu(onEditClick, onDeleteClick)
            }

            FinancialLayoutWidth.EXPANDED -> {
                Text(
                    text = financialTableEntry.categoryName,
                    modifier = Modifier.weight(2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FinancialEntryAmount(financialTableEntry, Modifier.weight(1.5f))
                Text(
                    text = financialTableEntry.paymentMethod?.let { paymentMethod -> stringResource(paymentMethod.title) }.orEmpty(),
                    modifier = Modifier.weight(1f),
                )
                financialTableEntry.occurredOn?.let { occurredOn ->
                    Text(occurredOn.format(FINANCIAL_DAY_FORMAT), Modifier.weight(1f))
                }
                FinancialEntryActionButtons(onEditClick, onDeleteClick)
            }
        }
    }
}

@Composable
private fun FinancialEntryAmount(financialTableEntry: FinancialTableEntry, modifier: Modifier = Modifier) {
    val amountDirection = financialTableEntry.amountDirection

    Text(
        text = when (amountDirection) {
            FinancialAmountDirection.INCOMING -> "↑ ${financialTableEntry.formattedAmount}"
            FinancialAmountDirection.OUTGOING -> "↓ ${financialTableEntry.formattedAmount}"
            null -> financialTableEntry.formattedAmount
        },
        modifier = modifier,
        color = when (amountDirection) {
            FinancialAmountDirection.INCOMING -> MaterialTheme.colorScheme.primary
            FinancialAmountDirection.OUTGOING -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.onSurface
        },
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun FinancialEntrySupportingText(supportingText: String) {
    Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FinancialEntryActionButtons(onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val editTitle = stringResource(Res.string.financialmanagement_action_edit)
    val deleteTitle = stringResource(Res.string.financialmanagement_action_delete)

    IconButton(onClick = onEditClick, modifier = Modifier.semantics { contentDescription = editTitle }) {
        Text("✎")
    }
    IconButton(onClick = onDeleteClick, modifier = Modifier.semantics { contentDescription = deleteTitle }) {
        Text("🗑")
    }
}

@Composable
private fun FinancialEntryActionsMenu(onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    var actionsMenuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { actionsMenuExpanded = true }) {
            Text("⋮")
        }
        DropdownMenu(expanded = actionsMenuExpanded, onDismissRequest = { actionsMenuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.financialmanagement_action_edit)) },
                onClick = {
                    actionsMenuExpanded = false
                    onEditClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.financialmanagement_action_delete)) },
                onClick = {
                    actionsMenuExpanded = false
                    onDeleteClick()
                },
            )
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialCategoryIcon.kt` — new

Emoji in a tinted circle; falls back to the first letter of the name.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun FinancialCategoryIcon(categoryIconName: String?, categoryName: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = categoryIconName ?: categoryName.take(1).uppercase(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialCategoryField.kt` — new

Read-only dropdown of existing categories, with each category's icon; shows a hint when the list is empty.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_field_category
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_field_category_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinancialCategoryField(
    selectedCategory: FinancialCategory?,
    selectableCategories: List<FinancialCategory>,
    onCategorySelect: (FinancialCategory) -> Unit,
) {
    var categoriesExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = categoriesExpanded, onExpandedChange = { expanded -> categoriesExpanded = expanded }) {
        OutlinedTextField(
            value = selectedCategory?.name.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(stringResource(Res.string.financialmanagement_field_category)) },
            leadingIcon = selectedCategory?.let { category ->
                @Composable { FinancialCategoryIcon(category.iconName, category.name) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) },
            supportingText = {
                if (selectableCategories.isEmpty()) {
                    Text(stringResource(Res.string.financialmanagement_field_category_empty))
                }
            },
            singleLine = true,
        )
        if (selectableCategories.isNotEmpty()) {
            ExposedDropdownMenu(expanded = categoriesExpanded, onDismissRequest = { categoriesExpanded = false }) {
                selectableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        leadingIcon = { FinancialCategoryIcon(category.iconName, category.name) },
                        onClick = {
                            onCategorySelect(category)
                            categoriesExpanded = false
                        },
                    )
                }
            }
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialMoneyField.kt` — new

Decimal keyboard, currency suffix, inline error state.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.ui.financialmanagement.common.parsePositiveMoney

@Composable
internal fun FinancialMoneyField(
    amountText: String,
    asset: AssetDefinition,
    title: String,
    onAmountTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(title) },
        suffix = { Text(asset.displayCode) },
        isError = amountText.isNotBlank() && parsePositiveMoney(amountText, asset) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialDateField.kt` — new

Material date picker; converts between UTC picker milliseconds and `LocalDate`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.datetime.*
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_cancel
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_select
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinancialDateField(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var datePickerVisible by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { datePickerVisible = true }, modifier = Modifier.fillMaxWidth()) {
        Text("📅 ${date.format(FINANCIAL_DAY_FORMAT)}")
    }

    if (datePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )

        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMilliseconds ->
                            onDateChange(Instant.fromEpochMilliseconds(selectedDateMilliseconds).toLocalDateTime(TimeZone.UTC).date)
                        }
                        datePickerVisible = false
                    },
                ) {
                    Text(stringResource(Res.string.financialmanagement_action_select))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(stringResource(Res.string.financialmanagement_action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialDialog.kt` — new

Generic dialog bound to a controller: title, scrollable content, rejection text, Save/Cancel.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

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
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_cancel
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.common.FinancialDialogController

@Composable
internal fun <Draft> FinancialDialog(
    financialDialogController: FinancialDialogController<Draft>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    content: @Composable (Draft) -> Unit,
) {
    val openedDialogState by financialDialogController.dialogState.collectAsStateWithLifecycle()
    val dialogState = openedDialogState ?: return

    AlertDialog(
        onDismissRequest = financialDialogController::dismiss,
        title = { Text(title(dialogState.draft)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content(dialogState.draft)
                dialogState.rejection?.let { rejection ->
                    Text(financialErrorText(rejection), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = financialDialogController::submit,
                enabled = dialogState.submittable && !dialogState.submitting,
            ) {
                Text(confirmationTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = financialDialogController::dismiss) {
                Text(stringResource(Res.string.financialmanagement_action_cancel))
            }
        },
    )
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialErrorText.kt` — new

`FinancialError` → localized text.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_error_conflict
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_error_not_found
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialError

@Composable
internal fun financialErrorText(financialError: FinancialError): String = when (financialError) {
    is FinancialError.Validation -> financialError.message
    is FinancialError.NotFound -> stringResource(Res.string.financialmanagement_error_not_found)
    is FinancialError.OperationConflict,
    is FinancialError.CategoryConflict,
    is FinancialError.PlanningConflict -> stringResource(Res.string.financialmanagement_error_conflict)

    is FinancialError.CategoryInUse,
    is FinancialError.CommandIdentifierReused,
    FinancialError.SessionClosed,
    FinancialError.StorageUnavailable,
    FinancialError.Forbidden -> stringResource(Res.string.financialmanagement_error_unsaved)
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialPaymentMethodTitle.kt` — new

`PaymentMethod` → string resource.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.financial.dto.PaymentMethod

internal val PaymentMethod.title: StringResource
    get() = when (this) {
        PaymentMethod.CARD -> Res.string.financialmanagement_payment_method_card
        PaymentMethod.CRYPTOCURRENCY -> Res.string.financialmanagement_payment_method_cryptocurrency
        PaymentMethod.CASH -> Res.string.financialmanagement_payment_method_cash
    }
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/component/FinancialDateFormats.kt` — new

`04.10.2026` and `10.2026` formats.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.component

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.char

internal val FINANCIAL_DAY_FORMAT = LocalDate.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
}

internal val FINANCIAL_MONTH_FORMAT = YearMonth.Format {
    monthNumber()
    char('.')
    year()
}
```

### Step 4 — Finance history (`history/`)

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialOperationRow.kt` — new

Row model; maps operation kind to the ↓/↑ direction.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.ui.financialmanagement.component.FinancialAmountDirection
import org.orev.nahidka.ui.financialmanagement.component.FinancialTableEntry

data class FinancialOperationRow(
    val operation: FinancialOperation,
    val category: FinancialCategory?,
    override val formattedAmount: String,
    override val occurredOn: LocalDate,
) : FinancialTableEntry {

    override val categoryIconName: String? get() = category?.iconName

    override val categoryName: String get() = category?.name.orEmpty()

    override val amountDirection: FinancialAmountDirection
        get() = when (operation.kind) {
            OperationKind.EXPENSE -> FinancialAmountDirection.OUTGOING
            OperationKind.INCOME, OperationKind.REFUND -> FinancialAmountDirection.INCOMING
        }

    override val paymentMethod: PaymentMethod get() = operation.paymentMethod
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryContent.kt` — new

Visible rows, `hasMoreRows`, categories.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.collections.immutable.PersistentList
import org.orev.nahidka.feature.financial.dto.FinancialCategory

data class FinancialHistoryContent(
    val rows: PersistentList<FinancialOperationRow>,
    val hasMoreRows: Boolean,
    val categories: PersistentList<FinancialCategory>,
)
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialOperationDraft.kt` — new

Editor draft with derived `amount` and `submittable` (requires a valid amount and a selected category).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.ui.financialmanagement.common.parsePositiveMoney

data class FinancialOperationDraft(
    val editedOperation: FinancialOperation?,
    val asset: AssetDefinition,
    val kind: OperationKind,
    val amountText: String,
    val category: FinancialCategory?,
    val paymentMethod: PaymentMethod,
    val occurredOn: LocalDate,
) {

    val amount: Money? get() = parsePositiveMoney(amountText, asset)

    val submittable: Boolean get() = amount != null && category != null
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialOperationKindTitle.kt` — new

Kinds offered on creation and their titles.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.collections.immutable.persistentListOf
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_kind_expense
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_kind_income
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_kind_refund
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.financial.dto.OperationKind

internal val FINANCIAL_OPERATION_CREATION_KINDS = persistentListOf(OperationKind.EXPENSE, OperationKind.INCOME)

internal val OperationKind.title: StringResource
    get() = when (this) {
        OperationKind.EXPENSE -> Res.string.financialmanagement_operation_kind_expense
        OperationKind.INCOME -> Res.string.financialmanagement_operation_kind_income
        OperationKind.REFUND -> Res.string.financialmanagement_operation_kind_refund
    }
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryViewModel.kt` — new

Currency selection, paging, editor and deletion dialogs. The occurrence time keeps the original (or current) time of day, so changing only the date never reorders same-day rows.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.financial.calculation.FinancialCategoryNameComparator
import org.orev.nahidka.feature.financial.calculation.FinancialOperationRecencyComparator
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.ui.financialmanagement.FinancialAssetSelection
import org.orev.nahidka.ui.financialmanagement.common.*
import kotlin.time.Instant

private const val FINANCIAL_HISTORY_PAGE_SIZE = 50

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class FinancialHistoryViewModel(
    financialSessionConfig: FinancialSessionConfig,
    private val financialGateway: FinancialGateway,
    private val financialAssetSelection: FinancialAssetSelection,
    private val identifierGenerator: IdentifierGenerator,
    private val applicationClock: ApplicationClock,
    private val exactMoneyFormatter: ExactMoneyFormatter,
) : ViewModel() {

    private val reportingTimeZone = TimeZone.of(financialSessionConfig.reportingTimeZone)

    private val displayedRowLimit = MutableStateFlow(FINANCIAL_HISTORY_PAGE_SIZE)

    val availableAssets: PersistentList<AssetDefinition> = financialAssetSelection.availableAssets

    val selectedAsset: StateFlow<AssetDefinition> = financialAssetSelection.selectedAsset

    val history: StateFlow<FinancialContentState<FinancialHistoryContent>> = financialAssetSelection.selectedAsset
        .flatMapLatest { selectedAsset -> observeHistory(selectedAsset) }
        .stateInFinancialContent(viewModelScope)

    val operationEditor = FinancialDialogController(viewModelScope, FinancialOperationDraft::submittable) { operationDraft ->
        val editedOperation = operationDraft.editedOperation

        if (editedOperation == null) {
            addOperation(operationDraft)
        } else {
            updateOperation(editedOperation, operationDraft)
        }
    }

    val operationDeletion = FinancialDialogController<FinancialOperationRow>(viewModelScope) { operationRow ->
        financialGateway.removeOperation(
            RemoveFinancialOperation(
                meta = identifierGenerator.nextCommandMeta(),
                identifier = operationRow.operation.identifier,
                expectedVersion = operationRow.operation.version,
            ),
        )
    }

    fun selectAsset(asset: AssetDefinition) {
        displayedRowLimit.value = FINANCIAL_HISTORY_PAGE_SIZE
        financialAssetSelection.select(asset)
    }

    fun loadNextPage() {
        displayedRowLimit.update { rowLimit -> rowLimit + FINANCIAL_HISTORY_PAGE_SIZE }
    }

    fun openOperationCreation() {
        operationEditor.open(
            FinancialOperationDraft(
                editedOperation = null,
                asset = selectedAsset.value,
                kind = OperationKind.EXPENSE,
                amountText = "",
                category = null,
                paymentMethod = PaymentMethod.CARD,
                occurredOn = applicationClock.now().toLocalDateTime(reportingTimeZone).date,
            ),
        )
    }

    fun openOperationEditing(operationRow: FinancialOperationRow) {
        operationEditor.open(
            FinancialOperationDraft(
                editedOperation = operationRow.operation,
                asset = selectedAsset.value,
                kind = operationRow.operation.kind,
                amountText = formatMoneyInput(operationRow.operation.amount, selectedAsset.value),
                category = operationRow.category,
                paymentMethod = operationRow.operation.paymentMethod,
                occurredOn = operationRow.occurredOn,
            ),
        )
    }

    fun openOperationDeletion(operationRow: FinancialOperationRow) {
        operationDeletion.open(operationRow)
    }

    private fun observeHistory(selectedAsset: AssetDefinition): Flow<FinancialHistoryContent> = combine(
        financialGateway
            .subscribeOperations(OperationQuery(assetIdentifier = selectedAsset.identifier))
            .observeEntities(FinancialOperation::identifier),
        financialGateway
            .subscribeCategories(CategoryQuery())
            .observeEntities(FinancialCategory::identifier),
        displayedRowLimit,
    ) { operationsByIdentifier, categoriesByIdentifier, rowLimit ->
        toHistoryContent(selectedAsset, operationsByIdentifier, categoriesByIdentifier, rowLimit)
    }

    private fun toHistoryContent(
        selectedAsset: AssetDefinition,
        operationsByIdentifier: PersistentMap<String, FinancialOperation>,
        categoriesByIdentifier: PersistentMap<String, FinancialCategory>,
        rowLimit: Int,
    ): FinancialHistoryContent {
        val sortedOperations = operationsByIdentifier.values.sortedWith(FinancialOperationRecencyComparator)

        return FinancialHistoryContent(
            rows = sortedOperations
                .take(rowLimit)
                .map { operation ->
                    FinancialOperationRow(
                        operation = operation,
                        category = categoriesByIdentifier[operation.categoryIdentifier],
                        formattedAmount = exactMoneyFormatter.format(operation.amount, selectedAsset),
                        occurredOn = operation.occurredAt.toLocalDateTime(reportingTimeZone).date,
                    )
                }
                .toPersistentList(),
            hasMoreRows = sortedOperations.size > rowLimit,
            categories = categoriesByIdentifier.values
                .sortedWith(FinancialCategoryNameComparator)
                .toPersistentList(),
        )
    }

    private suspend fun addOperation(operationDraft: FinancialOperationDraft): MutationResult<FinancialOperation> =
        financialGateway.addOperation(
            AddFinancialOperation(
                meta = identifierGenerator.nextCommandMeta(),
                operation = NewFinancialOperation(
                    identifier = identifierGenerator.next(),
                    amount = checkNotNull(operationDraft.amount),
                    kind = operationDraft.kind,
                    categoryIdentifier = checkNotNull(operationDraft.category).identifier,
                    paymentMethod = operationDraft.paymentMethod,
                    occurredAt = occurredAt(operationDraft),
                ),
            ),
        )

    private suspend fun updateOperation(
        editedOperation: FinancialOperation,
        operationDraft: FinancialOperationDraft,
    ): MutationResult<FinancialOperation> = financialGateway.updateOperation(
        UpdateFinancialOperation(
            meta = identifierGenerator.nextCommandMeta(),
            identifier = editedOperation.identifier,
            expectedVersion = editedOperation.version,
            patch = FinancialOperationPatch(
                amount = operationDraft.amount,
                categoryIdentifier = NullablePatch.Set(checkNotNull(operationDraft.category).identifier),
                paymentMethod = operationDraft.paymentMethod,
                occurredAt = occurredAt(operationDraft),
            ),
        ),
    )

    private fun occurredAt(operationDraft: FinancialOperationDraft): Instant {
        val referenceInstant = operationDraft.editedOperation?.occurredAt ?: applicationClock.now()
        val referenceDateTime = referenceInstant.toLocalDateTime(reportingTimeZone)

        return if (referenceDateTime.date == operationDraft.occurredOn) {
            referenceInstant
        } else {
            operationDraft.occurredOn
                .atTime(referenceDateTime.time)
                .toInstant(reportingTimeZone)
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryTab.kt` — new

Header actions, table, and the paging footer (the `LaunchedEffect` fires when the footer becomes visible).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_history_empty
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_history_end
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_add
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialHistoryTab(
    financialHistoryViewModel: FinancialHistoryViewModel,
    financialLayoutWidth: FinancialLayoutWidth,
    tabSelector: @Composable () -> Unit,
) {
    val selectedAsset by financialHistoryViewModel.selectedAsset.collectAsStateWithLifecycle()
    val historyContentState by financialHistoryViewModel.history.collectAsStateWithLifecycle()

    FinancialTabLayout(
        financialLayoutWidth = financialLayoutWidth,
        tabSelector = tabSelector,
        actions = {
            FinancialChoiceSelector(
                options = financialHistoryViewModel.availableAssets,
                selectedOption = selectedAsset,
                optionTitle = { asset -> asset.displayCode },
                onOptionSelect = financialHistoryViewModel::selectAsset,
            )
            FinancialAddButton(
                title = stringResource(Res.string.financialmanagement_operation_add),
                financialLayoutWidth = financialLayoutWidth,
                onClick = financialHistoryViewModel::openOperationCreation,
            )
        },
    ) {
        FinancialContent(historyContentState) { historyContent ->
            FinancialTable(
                rows = historyContent.rows,
                rowKey = { operationRow -> operationRow.operation.identifier },
                emptyTableMessage = stringResource(Res.string.financialmanagement_history_empty),
                footer = { FinancialHistoryFooter(historyContent, financialHistoryViewModel::loadNextPage) },
            ) { operationRow ->
                FinancialTableRow(
                    financialTableEntry = operationRow,
                    financialLayoutWidth = financialLayoutWidth,
                    onEditClick = { financialHistoryViewModel.openOperationEditing(operationRow) },
                    onDeleteClick = { financialHistoryViewModel.openOperationDeletion(operationRow) },
                )
            }
        }
    }
}

@Composable
private fun FinancialHistoryFooter(historyContent: FinancialHistoryContent, onEndReached: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (historyContent.hasMoreRows) {
            LaunchedEffect(historyContent.rows.size) { onEndReached() }
            CircularProgressIndicator()
        } else {
            Text(stringResource(Res.string.financialmanagement_history_end), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryDialogs.kt` — new

Operation editor and deletion confirmation.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.ui.financialmanagement.common.availableContentOrNull
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialHistoryDialogs(financialHistoryViewModel: FinancialHistoryViewModel) {
    val historyContentState by financialHistoryViewModel.history.collectAsStateWithLifecycle()
    val selectableCategories = historyContentState
        .availableContentOrNull()
        ?.categories
        .orEmpty()
        .filterNot(FinancialCategory::archived)

    FinancialDialog(
        financialDialogController = financialHistoryViewModel.operationEditor,
        title = { operationDraft ->
            stringResource(
                if (operationDraft.editedOperation == null) {
                    Res.string.financialmanagement_operation_creation_title
                } else {
                    Res.string.financialmanagement_operation_editing_title
                },
            )
        },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_save),
    ) { operationDraft ->
        FinancialOperationForm(
            operationDraft = operationDraft,
            selectableCategories = selectableCategories,
            onOperationDraftEdit = financialHistoryViewModel.operationEditor::edit,
        )
    }

    FinancialDialog(
        financialDialogController = financialHistoryViewModel.operationDeletion,
        title = { stringResource(Res.string.financialmanagement_operation_deletion_title) },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_delete),
    ) { operationRow ->
        Text(
            stringResource(
                Res.string.financialmanagement_operation_deletion_message,
                operationRow.categoryName,
                operationRow.formattedAmount,
            ),
        )
    }
}

@Composable
private fun FinancialOperationForm(
    operationDraft: FinancialOperationDraft,
    selectableCategories: List<FinancialCategory>,
    onOperationDraftEdit: ((FinancialOperationDraft) -> FinancialOperationDraft) -> Unit,
) {
    if (operationDraft.editedOperation == null) {
        FinancialChoiceSelector(
            options = FINANCIAL_OPERATION_CREATION_KINDS,
            selectedOption = operationDraft.kind,
            optionTitle = { operationKind -> stringResource(operationKind.title) },
            onOptionSelect = { operationKind -> onOperationDraftEdit { draft -> draft.copy(kind = operationKind) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    FinancialMoneyField(
        amountText = operationDraft.amountText,
        asset = operationDraft.asset,
        title = stringResource(Res.string.financialmanagement_field_amount),
        onAmountTextChange = { amountText -> onOperationDraftEdit { draft -> draft.copy(amountText = amountText) } },
    )
    FinancialCategoryField(
        selectedCategory = operationDraft.category,
        selectableCategories = selectableCategories,
        onCategorySelect = { category -> onOperationDraftEdit { draft -> draft.copy(category = category) } },
    )
    FinancialChoiceSelector(
        options = PaymentMethod.entries,
        selectedOption = operationDraft.paymentMethod,
        optionTitle = { paymentMethod -> stringResource(paymentMethod.title) },
        onOptionSelect = { paymentMethod -> onOperationDraftEdit { draft -> draft.copy(paymentMethod = paymentMethod) } },
        modifier = Modifier.fillMaxWidth(),
    )
    FinancialDateField(
        date = operationDraft.occurredOn,
        onDateChange = { occurredOn -> onOperationDraftEdit { draft -> draft.copy(occurredOn = occurredOn) } },
    )
}
```

### Step 5 — Finance planning (`planning/`)

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningSummary.kt` — new

Three formatted summary values.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

data class FinancialPlanningSummary(
    val formattedAvailableAmount: String,
    val formattedRemainingAmount: String,
    val formattedSavableAmount: String,
)
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningRow.kt` — new

Row model (no direction, no date).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.PlanningRow
import org.orev.nahidka.ui.financialmanagement.component.FinancialAmountDirection
import org.orev.nahidka.ui.financialmanagement.component.FinancialTableEntry

data class FinancialPlanningRow(
    val planningRow: PlanningRow,
    val category: FinancialCategory?,
    override val categoryName: String,
    override val formattedAmount: String,
) : FinancialTableEntry {

    override val categoryIconName: String? get() = category?.iconName

    override val amountDirection: FinancialAmountDirection? get() = null

    override val paymentMethod: PaymentMethod? get() = planningRow.preferredPaymentMethod

    override val occurredOn: LocalDate? get() = null
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningContent.kt` — new

Month, currency, planning document, summary, rows, categories.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.collections.immutable.PersistentList
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable

data class FinancialPlanningContent(
    val month: YearMonth,
    val asset: AssetDefinition,
    val planningTable: FinancialPlanningTable?,
    val summary: FinancialPlanningSummary,
    val rows: PersistentList<FinancialPlanningRow>,
    val categories: PersistentList<FinancialCategory>,
)
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningRowDraft.kt` — new

Editor/deletion draft; `selectableCategories` hides archived categories and those already planned this month.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.ui.financialmanagement.common.parsePositiveMoney

data class FinancialPlanningRowDraft(
    val planningTable: FinancialPlanningTable?,
    val month: YearMonth,
    val asset: AssetDefinition,
    val rowIdentifier: String,
    val category: FinancialCategory?,
    val plannedAmountText: String,
    val preferredPaymentMethod: PaymentMethod,
) {

    val creation: Boolean
        get() = planningTable?.rows.orEmpty().none { planningRow -> planningRow.identifier == rowIdentifier }

    val plannedAmount: Money? get() = parsePositiveMoney(plannedAmountText, asset)

    val submittable: Boolean get() = plannedAmount != null && category != null

    fun selectableCategories(categories: List<FinancialCategory>): List<FinancialCategory> {
        val plannedCategoryIdentifiers = planningTable?.rows.orEmpty()
            .filterNot { planningRow -> planningRow.identifier == rowIdentifier }
            .map(PlanningRow::categoryIdentifier)
            .toSet()

        return categories.filter { category ->
            !category.archived && category.identifier !in plannedCategoryIdentifiers
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningViewModel.kt` — new

Month navigation; add, edit and delete row via one `savePlanningRows` path (it creates the table on the first row).

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.toInput
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter
import org.orev.nahidka.ui.financialmanagement.FinancialAssetSelection
import org.orev.nahidka.ui.financialmanagement.common.*

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class FinancialPlanningViewModel(
    financialSessionConfig: FinancialSessionConfig,
    applicationClock: ApplicationClock,
    financialAssetSelection: FinancialAssetSelection,
    private val financialGateway: FinancialGateway,
    private val identifierGenerator: IdentifierGenerator,
    private val exactMoneyFormatter: ExactMoneyFormatter,
) : ViewModel() {

    private val mutableSelectedMonth = MutableStateFlow(
        financialMonthFor(applicationClock.now(), financialSessionConfig.reportingTimeZone),
    )

    val selectedMonth: StateFlow<YearMonth> = mutableSelectedMonth.asStateFlow()

    val planning: StateFlow<FinancialContentState<FinancialPlanningContent>> =
        combine(mutableSelectedMonth, financialAssetSelection.selectedAsset) { month, asset ->
            MonthlyQuery(month, asset.identifier)
        }
            .flatMapLatest(financialGateway::observeFinancialSnapshot)
            .map(::toPlanningContent)
            .stateInFinancialContent(viewModelScope)

    val planningRowEditor = FinancialDialogController(viewModelScope, FinancialPlanningRowDraft::submittable) { planningRowDraft ->
        savePlanningRows(planningRowDraft) { planningRows ->
            planningRows.withPlanningRow(toPlanningRow(planningRowDraft))
        }
    }

    val planningRowDeletion = FinancialDialogController<FinancialPlanningRowDraft>(viewModelScope) { planningRowDraft ->
        savePlanningRows(planningRowDraft) { planningRows ->
            planningRows.removingAll { planningRow -> planningRow.identifier == planningRowDraft.rowIdentifier }
        }
    }

    fun selectPreviousMonth() {
        mutableSelectedMonth.update(YearMonth::minusMonth)
    }

    fun selectNextMonth() {
        mutableSelectedMonth.update(YearMonth::plusMonth)
    }

    fun openPlanningRowCreation() {
        openPlanningRowDialog(planningRowEditor, planningRow = null)
    }

    fun openPlanningRowEditing(planningRow: FinancialPlanningRow) {
        openPlanningRowDialog(planningRowEditor, planningRow)
    }

    fun openPlanningRowDeletion(planningRow: FinancialPlanningRow) {
        openPlanningRowDialog(planningRowDeletion, planningRow)
    }

    private fun openPlanningRowDialog(
        financialDialogController: FinancialDialogController<FinancialPlanningRowDraft>,
        planningRow: FinancialPlanningRow?,
    ) {
        val planningContent = planning.value.availableContentOrNull() ?: return

        financialDialogController.open(
            FinancialPlanningRowDraft(
                planningTable = planningContent.planningTable,
                month = planningContent.month,
                asset = planningContent.asset,
                rowIdentifier = planningRow?.planningRow?.identifier ?: identifierGenerator.next(),
                category = planningRow?.category,
                plannedAmountText = planningRow
                    ?.let { existingRow -> formatMoneyInput(existingRow.planningRow.plannedAmount, planningContent.asset) }
                    .orEmpty(),
                preferredPaymentMethod = planningRow?.planningRow?.preferredPaymentMethod ?: PaymentMethod.CARD,
            ),
        )
    }

    private fun toPlanningContent(financialSnapshot: FinancialSnapshot): FinancialPlanningContent {
        val planningTableView = (financialSnapshot.planning as? PlanningConfigured)?.table
        val categoriesByIdentifier = financialSnapshot.categories.associateBy(FinancialCategory::identifier)

        return FinancialPlanningContent(
            month = financialSnapshot.period.month,
            asset = financialSnapshot.asset,
            planningTable = planningTableView?.document,
            summary = toPlanningSummary(financialSnapshot.planning, financialSnapshot.asset),
            rows = planningTableView?.rows.orEmpty()
                .map { planningRowView ->
                    FinancialPlanningRow(
                        planningRow = planningRowView.input,
                        category = categoriesByIdentifier[planningRowView.input.categoryIdentifier],
                        categoryName = planningRowView.categoryName,
                        formattedAmount = exactMoneyFormatter.format(planningRowView.input.plannedAmount, financialSnapshot.asset),
                    )
                }
                .toPersistentList(),
            categories = financialSnapshot.categories,
        )
    }

    private fun toPlanningSummary(planningState: PlanningState, asset: AssetDefinition): FinancialPlanningSummary =
        when (planningState) {
            is PlanningConfigured -> toPlanningSummary(
                asset = asset,
                availableAmount = planningState.table.totals.fundsAvailableThisMonth,
                remainingAmount = planningState.table.totals.currentAvailable,
                savableAmount = planningState.table.totals.projectedAvailableAfterPlanning,
            )

            is PlanningNotConfigured -> toPlanningSummary(
                asset = asset,
                availableAmount = planningState.income,
                remainingAmount = planningState.currentAvailable,
                savableAmount = planningState.currentAvailable,
            )
        }

    private fun toPlanningSummary(
        asset: AssetDefinition,
        availableAmount: Money,
        remainingAmount: Money,
        savableAmount: Money,
    ): FinancialPlanningSummary = FinancialPlanningSummary(
        formattedAvailableAmount = exactMoneyFormatter.format(availableAmount, asset),
        formattedRemainingAmount = exactMoneyFormatter.format(remainingAmount, asset),
        formattedSavableAmount = exactMoneyFormatter.format(savableAmount, asset),
    )

    private fun toPlanningRow(planningRowDraft: FinancialPlanningRowDraft): PlanningRow =
        PlanningRow(
            identifier = planningRowDraft.rowIdentifier,
            categoryIdentifier = checkNotNull(planningRowDraft.category).identifier,
            plannedAmount = checkNotNull(planningRowDraft.plannedAmount),
            included = true,
            preferredPaymentMethod = planningRowDraft.preferredPaymentMethod,
        )

    private suspend fun savePlanningRows(
        planningRowDraft: FinancialPlanningRowDraft,
        planningRowsTransformation: (PersistentList<PlanningRow>) -> PersistentList<PlanningRow>,
    ): MutationResult<PlanningTableView> {
        val planningTableInput = planningRowDraft.planningTable?.toInput() ?: FinancialPlanningTableInput(
            identifier = identifierGenerator.next(),
            month = planningRowDraft.month,
            assetIdentifier = planningRowDraft.asset.identifier,
            openingAvailable = Money(planningRowDraft.asset.identifier, 0),
            savingsPolicy = null,
            rows = persistentListOf(),
        )

        return financialGateway.savePlanningTable(
            SavePlanningTable(
                meta = identifierGenerator.nextCommandMeta(),
                expectedVersion = planningRowDraft.planningTable?.version,
                table = planningTableInput.copy(rows = planningRowsTransformation(planningTableInput.rows)),
            ),
        )
    }

    private fun PersistentList<PlanningRow>.withPlanningRow(planningRow: PlanningRow): PersistentList<PlanningRow> {
        val existingRowIndex = indexOfFirst { existingRow -> existingRow.identifier == planningRow.identifier }

        return if (existingRowIndex == -1) {
            adding(planningRow)
        } else {
            replacingAt(existingRowIndex, planningRow)
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialMonthSelector.kt` — new

`‹ 10.2026 ›`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_planning_next_month
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_planning_previous_month
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.FINANCIAL_MONTH_FORMAT

@Composable
internal fun FinancialMonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
) {
    val previousMonthTitle = stringResource(Res.string.financialmanagement_planning_previous_month)
    val nextMonthTitle = stringResource(Res.string.financialmanagement_planning_next_month)

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onPreviousMonthClick,
            modifier = Modifier.semantics { contentDescription = previousMonthTitle },
        ) {
            Text("‹")
        }
        Text(selectedMonth.format(FINANCIAL_MONTH_FORMAT), style = MaterialTheme.typography.titleMedium)
        IconButton(
            onClick = onNextMonthClick,
            modifier = Modifier.semantics { contentDescription = nextMonthTitle },
        ) {
            Text("›")
        }
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningTab.kt` — new

Header actions, summary lines, table.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialPlanningTab(
    financialPlanningViewModel: FinancialPlanningViewModel,
    financialLayoutWidth: FinancialLayoutWidth,
    tabSelector: @Composable () -> Unit,
) {
    val selectedMonth by financialPlanningViewModel.selectedMonth.collectAsStateWithLifecycle()
    val planningContentState by financialPlanningViewModel.planning.collectAsStateWithLifecycle()

    FinancialTabLayout(
        financialLayoutWidth = financialLayoutWidth,
        tabSelector = tabSelector,
        actions = {
            FinancialMonthSelector(
                selectedMonth = selectedMonth,
                onPreviousMonthClick = financialPlanningViewModel::selectPreviousMonth,
                onNextMonthClick = financialPlanningViewModel::selectNextMonth,
            )
            FinancialAddButton(
                title = stringResource(Res.string.financialmanagement_planning_add),
                financialLayoutWidth = financialLayoutWidth,
                onClick = financialPlanningViewModel::openPlanningRowCreation,
            )
        },
    ) {
        FinancialContent(planningContentState) { planningContent ->
            FinancialPlanningSummaryText(planningContent.summary)
            FinancialTable(
                rows = planningContent.rows,
                rowKey = { planningRow -> planningRow.planningRow.identifier },
                emptyTableMessage = stringResource(Res.string.financialmanagement_planning_empty),
            ) { planningRow ->
                FinancialTableRow(
                    financialTableEntry = planningRow,
                    financialLayoutWidth = financialLayoutWidth,
                    onEditClick = { financialPlanningViewModel.openPlanningRowEditing(planningRow) },
                    onDeleteClick = { financialPlanningViewModel.openPlanningRowDeletion(planningRow) },
                )
            }
        }
    }
}

@Composable
private fun FinancialPlanningSummaryText(financialPlanningSummary: FinancialPlanningSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.financialmanagement_planning_available, financialPlanningSummary.formattedAvailableAmount),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.financialmanagement_planning_remaining, financialPlanningSummary.formattedRemainingAmount),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.financialmanagement_planning_savable, financialPlanningSummary.formattedSavableAmount),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningDialogs.kt` — new

Planning-row editor and deletion confirmation.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.ui.financialmanagement.common.availableContentOrNull
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialPlanningDialogs(financialPlanningViewModel: FinancialPlanningViewModel) {
    val planningContentState by financialPlanningViewModel.planning.collectAsStateWithLifecycle()
    val categories = planningContentState.availableContentOrNull()?.categories.orEmpty()

    FinancialDialog(
        financialDialogController = financialPlanningViewModel.planningRowEditor,
        title = { planningRowDraft ->
            stringResource(
                if (planningRowDraft.creation) {
                    Res.string.financialmanagement_planning_creation_title
                } else {
                    Res.string.financialmanagement_planning_editing_title
                },
            )
        },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_save),
    ) { planningRowDraft ->
        FinancialPlanningRowForm(
            planningRowDraft = planningRowDraft,
            selectableCategories = planningRowDraft.selectableCategories(categories),
            onPlanningRowDraftEdit = financialPlanningViewModel.planningRowEditor::edit,
        )
    }

    FinancialDialog(
        financialDialogController = financialPlanningViewModel.planningRowDeletion,
        title = { stringResource(Res.string.financialmanagement_planning_deletion_title) },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_delete),
    ) { planningRowDraft ->
        Text(stringResource(Res.string.financialmanagement_planning_deletion_message, planningRowDraft.category?.name.orEmpty()))
    }
}

@Composable
private fun FinancialPlanningRowForm(
    planningRowDraft: FinancialPlanningRowDraft,
    selectableCategories: List<FinancialCategory>,
    onPlanningRowDraftEdit: ((FinancialPlanningRowDraft) -> FinancialPlanningRowDraft) -> Unit,
) {
    FinancialCategoryField(
        selectedCategory = planningRowDraft.category,
        selectableCategories = selectableCategories,
        onCategorySelect = { category -> onPlanningRowDraftEdit { draft -> draft.copy(category = category) } },
    )
    FinancialMoneyField(
        amountText = planningRowDraft.plannedAmountText,
        asset = planningRowDraft.asset,
        title = stringResource(Res.string.financialmanagement_field_planned_amount),
        onAmountTextChange = { plannedAmountText -> onPlanningRowDraftEdit { draft -> draft.copy(plannedAmountText = plannedAmountText) } },
    )
    FinancialChoiceSelector(
        options = PaymentMethod.entries,
        selectedOption = planningRowDraft.preferredPaymentMethod,
        optionTitle = { paymentMethod -> stringResource(paymentMethod.title) },
        onOptionSelect = { paymentMethod -> onPlanningRowDraftEdit { draft -> draft.copy(preferredPaymentMethod = paymentMethod) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
```

### Step 6 — Screen entry point

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialManagementTab.kt` — new

Tab enum with titles.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement

import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_tab_history
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_tab_planning
import org.jetbrains.compose.resources.StringResource

internal enum class FinancialManagementTab(val title: StringResource) {
    HISTORY(Res.string.financialmanagement_tab_history),
    PLANNING(Res.string.financialmanagement_tab_planning),
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialAssetSelection.kt` — new

Session-scoped currency selection.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig

@Inject
@SingleIn(FinancialSessionScope::class)
class FinancialAssetSelection(financialSessionConfig: FinancialSessionConfig) {

    val availableAssets: PersistentList<AssetDefinition> = financialSessionConfig.assets

    private val mutableSelectedAsset = MutableStateFlow(
        availableAssets.first { asset -> asset.identifier == financialSessionConfig.defaultAssetIdentifier },
    )

    val selectedAsset: StateFlow<AssetDefinition> = mutableSelectedAsset.asStateFlow()

    fun select(asset: AssetDefinition) {
        mutableSelectedAsset.value = asset
    }
}
```

#### `shared/ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialManagementScreen.kt` — new

Public entry point: width class, tab selector, both tabs, both dialog hosts. Dialogs live at screen level, so "Add Expense" from the dashboard works whichever tab was last open.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.FinancialChoiceSelector
import org.orev.nahidka.ui.financialmanagement.component.FinancialLayoutWidth
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryDialogs
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryTab
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningDialogs
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningTab
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel

@Composable
fun FinancialManagementScreen(
    financialHistoryViewModel: FinancialHistoryViewModel,
    financialPlanningViewModel: FinancialPlanningViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(FinancialManagementTab.HISTORY) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val financialLayoutWidth = FinancialLayoutWidth.of(maxWidth)
        val tabSelector = @Composable {
            FinancialChoiceSelector(
                options = FinancialManagementTab.entries,
                selectedOption = selectedTab,
                optionTitle = { financialManagementTab -> stringResource(financialManagementTab.title) },
                onOptionSelect = { financialManagementTab -> selectedTab = financialManagementTab },
                modifier = if (financialLayoutWidth == FinancialLayoutWidth.COMPACT) Modifier.fillMaxWidth() else Modifier,
            )
        }

        when (selectedTab) {
            FinancialManagementTab.HISTORY -> FinancialHistoryTab(financialHistoryViewModel, financialLayoutWidth, tabSelector)
            FinancialManagementTab.PLANNING -> FinancialPlanningTab(financialPlanningViewModel, financialLayoutWidth, tabSelector)
        }
    }

    FinancialHistoryDialogs(financialHistoryViewModel)
    FinancialPlanningDialogs(financialPlanningViewModel)
}
```

### Step 7 — String resources

All UI text is localized. `uk` and `ru` are fully translated (they previously contained only the title).

#### `shared/ui/financial-management/src/commonMain/composeResources/values/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_financialmanagement_title">Finances</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_financialmanagement_title">Finances</string>
    <string name="financialmanagement_tab_history">Finance history</string>
    <string name="financialmanagement_tab_planning">Finance planning</string>
    <string name="financialmanagement_action_save">Save</string>
    <string name="financialmanagement_action_cancel">Cancel</string>
    <string name="financialmanagement_action_edit">Edit</string>
    <string name="financialmanagement_action_delete">Delete</string>
    <string name="financialmanagement_action_select">Select</string>
    <string name="financialmanagement_content_unavailable">Financial data is unavailable</string>
    <string name="financialmanagement_field_amount">Amount</string>
    <string name="financialmanagement_field_planned_amount">Planned amount</string>
    <string name="financialmanagement_field_category">Category</string>
    <string name="financialmanagement_field_category_empty">There are no categories yet. Create one on the categories page</string>
    <string name="financialmanagement_payment_method_card">Card</string>
    <string name="financialmanagement_payment_method_cryptocurrency">Crypto</string>
    <string name="financialmanagement_payment_method_cash">Cash</string>
    <string name="financialmanagement_operation_kind_expense">Expense</string>
    <string name="financialmanagement_operation_kind_income">Income</string>
    <string name="financialmanagement_operation_kind_refund">Refund</string>
    <string name="financialmanagement_operation_add">Add financial operation</string>
    <string name="financialmanagement_operation_creation_title">New financial operation</string>
    <string name="financialmanagement_operation_editing_title">Edit financial operation</string>
    <string name="financialmanagement_operation_deletion_title">Delete financial operation?</string>
    <string name="financialmanagement_operation_deletion_message">%1$s · %2$s will be deleted.</string>
    <string name="financialmanagement_history_empty">There are no transactions yet</string>
    <string name="financialmanagement_history_end">That is all data</string>
    <string name="financialmanagement_planning_add">Add category for planning</string>
    <string name="financialmanagement_planning_creation_title">New planning category</string>
    <string name="financialmanagement_planning_editing_title">Edit planning category</string>
    <string name="financialmanagement_planning_deletion_title">Delete planning category?</string>
    <string name="financialmanagement_planning_deletion_message">%1$s will be removed from this month plan.</string>
    <string name="financialmanagement_planning_empty">There are no planning categories for this month yet</string>
    <string name="financialmanagement_planning_available">Amount of money: %1$s</string>
    <string name="financialmanagement_planning_remaining">How much is left: %1$s</string>
    <string name="financialmanagement_planning_savable">Can save money: ~%1$s</string>
    <string name="financialmanagement_planning_previous_month">Previous month</string>
    <string name="financialmanagement_planning_next_month">Next month</string>
    <string name="financialmanagement_error_not_found">This record no longer exists</string>
    <string name="financialmanagement_error_conflict">This record was changed elsewhere. Close the dialog and try again</string>
    <string name="financialmanagement_error_unsaved">This change could not be saved</string>
</resources>
```

#### `shared/ui/financial-management/src/commonMain/composeResources/values-uk/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_financialmanagement_title">Фінанси</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_financialmanagement_title">Фінанси</string>
    <string name="financialmanagement_tab_history">Історія фінансів</string>
    <string name="financialmanagement_tab_planning">Планування фінансів</string>
    <string name="financialmanagement_action_save">Зберегти</string>
    <string name="financialmanagement_action_cancel">Скасувати</string>
    <string name="financialmanagement_action_edit">Редагувати</string>
    <string name="financialmanagement_action_delete">Видалити</string>
    <string name="financialmanagement_action_select">Вибрати</string>
    <string name="financialmanagement_content_unavailable">Фінансові дані недоступні</string>
    <string name="financialmanagement_field_amount">Сума</string>
    <string name="financialmanagement_field_planned_amount">Запланована сума</string>
    <string name="financialmanagement_field_category">Категорія</string>
    <string name="financialmanagement_field_category_empty">Категорій ще немає. Створіть категорію на сторінці категорій</string>
    <string name="financialmanagement_payment_method_card">Картка</string>
    <string name="financialmanagement_payment_method_cryptocurrency">Крипта</string>
    <string name="financialmanagement_payment_method_cash">Готівка</string>
    <string name="financialmanagement_operation_kind_expense">Витрата</string>
    <string name="financialmanagement_operation_kind_income">Дохід</string>
    <string name="financialmanagement_operation_kind_refund">Повернення</string>
    <string name="financialmanagement_operation_add">Додати фінансову операцію</string>
    <string name="financialmanagement_operation_creation_title">Нова фінансова операція</string>
    <string name="financialmanagement_operation_editing_title">Редагування фінансової операції</string>
    <string name="financialmanagement_operation_deletion_title">Видалити фінансову операцію?</string>
    <string name="financialmanagement_operation_deletion_message">%1$s · %2$s буде видалено.</string>
    <string name="financialmanagement_history_empty">Транзакцій ще немає</string>
    <string name="financialmanagement_history_end">Це всі дані</string>
    <string name="financialmanagement_planning_add">Додати категорію для планування</string>
    <string name="financialmanagement_planning_creation_title">Нова категорія планування</string>
    <string name="financialmanagement_planning_editing_title">Редагування категорії планування</string>
    <string name="financialmanagement_planning_deletion_title">Видалити категорію планування?</string>
    <string name="financialmanagement_planning_deletion_message">%1$s буде видалено з плану цього місяця.</string>
    <string name="financialmanagement_planning_empty">Категорій планування на цей місяць ще немає</string>
    <string name="financialmanagement_planning_available">Кількість грошей: %1$s</string>
    <string name="financialmanagement_planning_remaining">Скільки залишилось: %1$s</string>
    <string name="financialmanagement_planning_savable">Можна заощадити: ~%1$s</string>
    <string name="financialmanagement_planning_previous_month">Попередній місяць</string>
    <string name="financialmanagement_planning_next_month">Наступний місяць</string>
    <string name="financialmanagement_error_not_found">Цей запис більше не існує</string>
    <string name="financialmanagement_error_conflict">Цей запис було змінено деінде. Закрийте діалог і спробуйте ще раз</string>
    <string name="financialmanagement_error_unsaved">Не вдалося зберегти зміни</string>
</resources>
```

#### `shared/ui/financial-management/src/commonMain/composeResources/values-ru/strings.xml` — modified

**Old:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_financialmanagement_title">Финансы</string>
</resources>
```

**New:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_financialmanagement_title">Финансы</string>
    <string name="financialmanagement_tab_history">История финансов</string>
    <string name="financialmanagement_tab_planning">Планирование финансов</string>
    <string name="financialmanagement_action_save">Сохранить</string>
    <string name="financialmanagement_action_cancel">Отмена</string>
    <string name="financialmanagement_action_edit">Редактировать</string>
    <string name="financialmanagement_action_delete">Удалить</string>
    <string name="financialmanagement_action_select">Выбрать</string>
    <string name="financialmanagement_content_unavailable">Финансовые данные недоступны</string>
    <string name="financialmanagement_field_amount">Сумма</string>
    <string name="financialmanagement_field_planned_amount">Запланированная сумма</string>
    <string name="financialmanagement_field_category">Категория</string>
    <string name="financialmanagement_field_category_empty">Категорий пока нет. Создайте категорию на странице категорий</string>
    <string name="financialmanagement_payment_method_card">Карта</string>
    <string name="financialmanagement_payment_method_cryptocurrency">Крипта</string>
    <string name="financialmanagement_payment_method_cash">Наличные</string>
    <string name="financialmanagement_operation_kind_expense">Расход</string>
    <string name="financialmanagement_operation_kind_income">Доход</string>
    <string name="financialmanagement_operation_kind_refund">Возврат</string>
    <string name="financialmanagement_operation_add">Добавить финансовую операцию</string>
    <string name="financialmanagement_operation_creation_title">Новая финансовая операция</string>
    <string name="financialmanagement_operation_editing_title">Редактирование финансовой операции</string>
    <string name="financialmanagement_operation_deletion_title">Удалить финансовую операцию?</string>
    <string name="financialmanagement_operation_deletion_message">%1$s · %2$s будет удалено.</string>
    <string name="financialmanagement_history_empty">Транзакций пока нет</string>
    <string name="financialmanagement_history_end">Это все данные</string>
    <string name="financialmanagement_planning_add">Добавить категорию для планирования</string>
    <string name="financialmanagement_planning_creation_title">Новая категория планирования</string>
    <string name="financialmanagement_planning_editing_title">Редактирование категории планирования</string>
    <string name="financialmanagement_planning_deletion_title">Удалить категорию планирования?</string>
    <string name="financialmanagement_planning_deletion_message">%1$s будет удалено из плана этого месяца.</string>
    <string name="financialmanagement_planning_empty">Категорий планирования на этот месяц пока нет</string>
    <string name="financialmanagement_planning_available">Количество денег: %1$s</string>
    <string name="financialmanagement_planning_remaining">Сколько осталось: %1$s</string>
    <string name="financialmanagement_planning_savable">Можно сэкономить: ~%1$s</string>
    <string name="financialmanagement_planning_previous_month">Предыдущий месяц</string>
    <string name="financialmanagement_planning_next_month">Следующий месяц</string>
    <string name="financialmanagement_error_not_found">Эта запись больше не существует</string>
    <string name="financialmanagement_error_conflict">Эта запись была изменена в другом месте. Закройте диалог и попробуйте снова</string>
    <string name="financialmanagement_error_unsaved">Не удалось сохранить изменения</string>
</resources>
```

### Step 8 — Tests (`src/jvmTest`)

The Metro test graph mirrors the host `FinancialSessionGraph`, so it also verifies DI wiring (including the shared `@SingleIn` currency selection). Test categories are created through the gateway, as the future categories page will do.

#### `shared/ui/financial-management/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialManagementTestGraph.kt` — new

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.persistentListOf
import org.orev.nahidka.core.common.*
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.gateway.FinancialGateway
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import kotlin.time.Instant

internal val FINANCIAL_TEST_ASSET = AssetDefinition("iso4217:USD", "USD", 2)

@DependencyGraph(FinancialSessionScope::class)
internal interface FinancialManagementTestGraph {
    val financialGateway: FinancialGateway
    val financialHistoryViewModel: FinancialHistoryViewModel
    val financialPlanningViewModel: FinancialPlanningViewModel

    @Provides
    @SingleIn(FinancialSessionScope::class)
    fun provideFinancialGateway(
        financialSessionConfig: FinancialSessionConfig,
        financialCalendar: FinancialCalendar,
    ): FinancialGateway = InMemoryFinancialGateway(financialSessionConfig, NoOpErrorReporter(), financialCalendar)

    @Provides
    fun provideFinancialSessionConfig(): FinancialSessionConfig = FinancialSessionConfig(
        sessionIdentity = "test-session",
        workspaceIdentity = "test-workspace",
        assets = persistentListOf(FINANCIAL_TEST_ASSET),
        defaultAssetIdentifier = FINANCIAL_TEST_ASSET.identifier,
        reportingTimeZone = "Europe/Kyiv",
    )

    @Provides
    fun provideApplicationClock(): ApplicationClock = ApplicationClock { Instant.parse("2026-10-04T12:00:00Z") }

    @Provides
    fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
```

#### `shared/ui/financial-management/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement/FinancialViewModelTest.kt` — new

Shared base: Main dispatcher setup, `createCategory`, `awaitContent`.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.ui.financialmanagement.common.FinancialContentState
import org.orev.nahidka.ui.financialmanagement.common.availableContentOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class FinancialViewModelTest {

    protected val financialManagementTestGraph = createGraph<FinancialManagementTestGraph>()

    protected val identifierGenerator = RandomIdentifierGenerator()

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected suspend fun createCategory(categoryName: String): FinancialCategory {
        val categoryCreation = financialManagementTestGraph.financialGateway.createCategory(
            CreateFinancialCategory(
                meta = CommandMeta(identifierGenerator.next()),
                identifier = identifierGenerator.next(),
                name = categoryName,
                iconName = "🍔",
            ),
        )

        return (categoryCreation as MutationResult.Committed).value
    }

    protected suspend fun <Content> StateFlow<FinancialContentState<Content>>.awaitContent(
        contentExpectation: (Content) -> Boolean,
    ): Content = checkNotNull(
        first { contentState -> contentState.availableContentOrNull()?.let(contentExpectation) == true }
            .availableContentOrNull(),
    )
}
```

#### `shared/ui/financial-management/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryViewModelTest.kt` — new

Create, Save disabled without a category, edit, delete, paging 50 → 51.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.coroutines.test.runTest
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.ui.financialmanagement.FINANCIAL_TEST_ASSET
import org.orev.nahidka.ui.financialmanagement.FinancialViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

internal class FinancialHistoryViewModelTest : FinancialViewModelTest() {

    private val financialHistoryViewModel = financialManagementTestGraph.financialHistoryViewModel

    @Test
    fun operationCreationAddsRow() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))

        val historyContent = financialHistoryViewModel.history.awaitContent { content ->
            content.rows.singleOrNull()?.categoryName == "Food"
        }

        assertEquals("12.50 USD", historyContent.rows.single().formattedAmount)
        assertNull(financialHistoryViewModel.operationEditor.dialogState.value)
    }

    @Test
    fun operationDraftWithoutCategoryIsNotSubmittable() = runTest {
        financialHistoryViewModel.openOperationCreation()
        financialHistoryViewModel.operationEditor.edit { operationDraft -> operationDraft.copy(amountText = "12.50") }

        assertFalse(checkNotNull(financialHistoryViewModel.operationEditor.dialogState.value).submittable)
    }

    @Test
    fun operationEditingUpdatesAmount() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))
        val createdRow = awaitCreatedRow()

        financialHistoryViewModel.openOperationEditing(createdRow)
        financialHistoryViewModel.operationEditor.edit { operationDraft -> operationDraft.copy(amountText = "20") }
        financialHistoryViewModel.operationEditor.submit()

        financialHistoryViewModel.history.awaitContent { content ->
            content.rows.singleOrNull()?.formattedAmount == "20.00 USD"
        }
    }

    @Test
    fun operationDeletionRemovesRow() = runTest {
        createOperation(amountText = "12.50", category = createCategory("Food"))
        val createdRow = awaitCreatedRow()

        financialHistoryViewModel.openOperationDeletion(createdRow)
        financialHistoryViewModel.operationDeletion.submit()

        financialHistoryViewModel.history.awaitContent { content -> content.rows.isEmpty() && !content.hasMoreRows }
    }

    @Test
    fun nextPageDisplaysRemainingRows() = runTest {
        val category = createCategory("Food")
        repeat(51) { operationIndex ->
            financialManagementTestGraph.financialGateway.addOperation(
                AddFinancialOperation(
                    meta = CommandMeta(identifierGenerator.next()),
                    operation = NewFinancialOperation(
                        identifier = identifierGenerator.next(),
                        amount = Money(FINANCIAL_TEST_ASSET.identifier, operationIndex + 1L),
                        kind = OperationKind.EXPENSE,
                        categoryIdentifier = category.identifier,
                        paymentMethod = PaymentMethod.CASH,
                        occurredAt = Instant.parse("2026-10-01T12:00:00Z"),
                    ),
                ),
            )
        }

        financialHistoryViewModel.history.awaitContent { content -> content.rows.size == 50 && content.hasMoreRows }
        financialHistoryViewModel.loadNextPage()

        financialHistoryViewModel.history.awaitContent { content -> content.rows.size == 51 && !content.hasMoreRows }
    }

    private suspend fun awaitCreatedRow(): FinancialOperationRow = financialHistoryViewModel.history
        .awaitContent { content -> content.rows.singleOrNull()?.categoryName == "Food" }
        .rows
        .single()

    private fun createOperation(amountText: String, category: FinancialCategory) {
        financialHistoryViewModel.openOperationCreation()
        financialHistoryViewModel.operationEditor.edit { operationDraft ->
            operationDraft.copy(amountText = amountText, category = category)
        }
        financialHistoryViewModel.operationEditor.submit()
    }
}
```

#### `shared/ui/financial-management/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningViewModelTest.kt` — new

First row creates the table (and the summary numbers), deletion keeps an empty table, month navigation.

**Old:** _(file does not exist)_

**New:**

```kotlin
package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.ui.financialmanagement.FinancialViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FinancialPlanningViewModelTest : FinancialViewModelTest() {

    private val financialPlanningViewModel = financialManagementTestGraph.financialPlanningViewModel

    @Test
    fun planningRowCreationCreatesMonthlyTable() = runTest {
        createPlanningRow(plannedAmountText = "300", category = createCategory("Food"))

        val planningContent = financialPlanningViewModel.planning.awaitContent { content -> content.rows.size == 1 }

        assertEquals("Food", planningContent.rows.single().categoryName)
        assertEquals("300.00 USD", planningContent.rows.single().formattedAmount)
        assertEquals(
            FinancialPlanningSummary(
                formattedAvailableAmount = "0.00 USD",
                formattedRemainingAmount = "0.00 USD",
                formattedSavableAmount = "-300.00 USD",
            ),
            planningContent.summary,
        )
    }

    @Test
    fun planningRowDeletionKeepsEmptyTable() = runTest {
        createPlanningRow(plannedAmountText = "300", category = createCategory("Food"))
        val createdRow = financialPlanningViewModel.planning.awaitContent { content -> content.rows.size == 1 }.rows.single()

        financialPlanningViewModel.openPlanningRowDeletion(createdRow)
        financialPlanningViewModel.planningRowDeletion.submit()

        financialPlanningViewModel.planning.awaitContent { content -> content.rows.isEmpty() && content.planningTable != null }
    }

    @Test
    fun nextMonthSelectionObservesNextMonth() = runTest {
        financialPlanningViewModel.selectNextMonth()

        assertEquals(YearMonth(2026, 11), financialPlanningViewModel.selectedMonth.value)
        financialPlanningViewModel.planning.awaitContent { content -> content.month == YearMonth(2026, 11) }
    }

    private suspend fun createPlanningRow(plannedAmountText: String, category: FinancialCategory) {
        financialPlanningViewModel.planning.awaitContent { true }
        financialPlanningViewModel.openPlanningRowCreation()
        financialPlanningViewModel.planningRowEditor.edit { planningRowDraft ->
            planningRowDraft.copy(plannedAmountText = plannedAmountText, category = category)
        }
        financialPlanningViewModel.planningRowEditor.submit()
    }
}
```

### Step 9 — Host integration (`shared/`)

Replaces the references to the deleted `FinancialManagementViewModel` / `FinancialManagementEvent`. *`:shared` does not compile today for unrelated reasons (see §5), so these two edits are verified only through the equivalent test graph.*

#### `shared/src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionGraph.kt` — modified

_Change 1 of 2 (old line 12 / new line 12)_

**Old:**

```kotlin
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.ui.dashboard.DashboardViewModel

@DependencyGraph(FinancialSessionScope::class)
```

**New:**

```kotlin
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel

@DependencyGraph(FinancialSessionScope::class)
```

_Change 2 of 2 (old line 18 / new line 20)_

**Old:**

```kotlin
    val gateway: FinancialGateway
    val dashboardViewModel: DashboardViewModel
    val financialManagementViewModel: FinancialManagementViewModel

    @Provides
```

**New:**

```kotlin
    val gateway: FinancialGateway
    val dashboardViewModel: DashboardViewModel
    val financialHistoryViewModel: FinancialHistoryViewModel
    val financialPlanningViewModel: FinancialPlanningViewModel

    @Provides
```

#### `shared/src/commonMain/kotlin/org/orev/nahidka/App.kt` — modified

_Change 1 of 4 (old line 46 / new line 46)_

**Old:**

```kotlin
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.settings.SettingsScreen

```

**New:**

```kotlin
import org.orev.nahidka.ui.dashboard.DashboardViewModel
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel
import org.orev.nahidka.ui.settings.SettingsScreen

```

_Change 2 of 4 (old line 113 / new line 115)_

**Old:**

```kotlin
    }

    val financialManagementViewModel = viewModel<FinancialManagementViewModel>(
        viewModelStoreOwner = session,
        key = "financial-management",
    ) {
        session.graph.financialManagementViewModel
    }

```

**New:**

```kotlin
    }

    val financialHistoryViewModel = viewModel<FinancialHistoryViewModel>(
        viewModelStoreOwner = session,
        key = "financial-history",
    ) {
        session.graph.financialHistoryViewModel
    }

    val financialPlanningViewModel = viewModel<FinancialPlanningViewModel>(
        viewModelStoreOwner = session,
        key = "financial-planning",
    ) {
        session.graph.financialPlanningViewModel
    }

```

_Change 3 of 4 (old line 166 / new line 175)_

**Old:**

```kotlin
                    )
                } else if (showingFinance) {
                    FinancialManagementScreen(financialManagementViewModel, onBack = { showingFinance = false })
                } else {
                    DashboardScreen(
```

**New:**

```kotlin
                    )
                } else if (showingFinance) {
                    FinancialManagementScreen(financialHistoryViewModel, financialPlanningViewModel)
                } else {
                    DashboardScreen(
```

_Change 4 of 4 (old line 175 / new line 184)_

**Old:**

```kotlin
                        onAddExpense = {
                            showingFinance = true
                            financialManagementViewModel.handleEvent(FinancialManagementEvent.OpenAddOperation)
                        },
                    )
```

**New:**

```kotlin
                        onAddExpense = {
                            showingFinance = true
                            financialHistoryViewModel.openOperationCreation()
                        },
                    )
```

---

## 5. Verification, limitations, out of scope

**Verified (in an isolated copy of the repository):**
* `:shared:core:feature:financial-management` and `:shared:ui:financial-management` compile for **JVM, JS, Wasm and Android**. The new code produces no compiler warnings, apart from the `Comparator` parameter-name note, which the existing `FinancialCategoryNameComparator` already triggers.
* `:shared:ui:financial-management:jvmTest`: **8/8 passing**, green on 3 consecutive forced re-runs. A deliberately broken assertion makes them fail, so they are not vacuous.
* Desktop (1000 dp) and phone (400 dp) layouts were rendered headlessly from this code: history, planning, empty month, the operation editor, the planning editor, and delete confirmation.

**Not verified:**
* **iOS**: it can't be built on this Linux host.
* **`:shared` (Step 9)**: this module doesn't compile today, independently of this plan. `App.kt`, `FinancialSessionGraph.kt` and `DashboardViewModel` reference modules deleted in `409e1fe`: `ui.common.theme.NahidkaTheme`, `ui.common.state.StateHolder`, `api.TaskStatus`, `ui.settings.SettingsScreen`, `GoalsTable`, `TasksTable` and `SocialBatteryWidget`. The DI wiring is covered by the test graph, which mirrors `FinancialSessionGraph`.

**Known limitations:**
* **Depends on the categories page (§2.2):** until it exists, no category can be created in the app, so nothing can be added on either tab.
* Paging is UI-side over the in-memory snapshot (decision §2.3).
* Opening balance and savings policy can't be edited (decision §2.1), and refunds can't be created (§2.5).

**Out of scope:** the categories page (create, rename, archive, delete; the core already supports all four), the side menu and app shell, restoring `ui/common`, and fixing `DashboardViewModel`. The dashboard could reuse the new `financialMonthFor(instant, …)` instead of building `YearMonth` by hand.

**Suggested implementation order:** Steps 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 (each step compiles on its own), then Step 9 once `:shared` builds again.
